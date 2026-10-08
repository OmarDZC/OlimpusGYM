package com.gymflow.booking;

import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservar y cancelar clases: el corazón del proyecto (LOGIC.md §5). El aforo nunca se
 * sobrevende porque cada reserva bloquea la fila de la sesión ({@link ClassSessionRepository#lockById})
 * durante toda la transacción: dos reservas concurrentes para la misma sesión se serializan ahí,
 * así que el recuento de plazas ocupadas que se lee después es siempre exacto.
 */
@Service
public class BookingService {

    private static final int CANCELLATION_WINDOW_HOURS = 2;

    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final Notifier notifier;

    public BookingService(
            ClassSessionRepository sessions,
            BookingRepository bookings,
            MembershipRepository memberships,
            PlanRepository plans,
            Notifier notifier) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.memberships = memberships;
        this.plans = plans;
        this.notifier = notifier;
    }

    @Transactional
    public Booking book(UUID tenantId, UUID userId, UUID sessionId) {
        ClassSession session = sessions.lockById(tenantId, sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        // Idempotencia (LOGIC.md §5.6): reservar dos veces la misma sesión no duplica.
        var existing = bookings.findActive(tenantId, userId, sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BookingNotAllowedException("Esta sesión está cancelada.");
        }
        if (!session.getStartsAt().isAfter(now)) {
            throw new BookingNotAllowedException("Esta sesión ya ha empezado o ha terminado.");
        }

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BookingNotAllowedException("No tienes una cuota activa."));
        Plan plan = plans.findById(membership.getPlanId())
                .orElseThrow(() -> new IllegalStateException("La tarifa de la membresía ya no existe"));

        Instant windowLimit = now.plus(plan.getBookingWindowDays(), ChronoUnit.DAYS);
        if (session.getStartsAt().isAfter(windowLimit)) {
            throw new BookingNotAllowedException(
                    "Tu tarifa permite reservar con " + plan.getBookingWindowDays()
                            + " días de antelación como mucho. Todavía no puedes reservar esta sesión.");
        }

        if (bookings.existsActiveOverlapping(tenantId, userId, session.getStartsAt(), session.getEndsAt())) {
            throw new BookingNotAllowedException("Ya tienes otra reserva a la misma hora.");
        }

        if (plan.getClassesPerMonth() != null) {
            YearMonth month = YearMonth.from(session.getStartsAt().atZone(ZoneOffset.UTC));
            Instant monthStart = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            long usedThisMonth = bookings.countBookedForSessionsInMonth(tenantId, userId, monthStart, monthEnd);
            if (usedThisMonth >= plan.getClassesPerMonth()) {
                throw new BookingNotAllowedException(
                        "Has llegado al límite de " + plan.getClassesPerMonth() + " reservas al mes de tu tarifa.");
            }
        }

        long bookedCount = bookings.countBySessionIdAndStatus(sessionId, BookingStatus.BOOKED);
        Booking booking;
        if (bookedCount < session.getCapacity()) {
            booking = new Booking(tenantId, sessionId, userId, BookingStatus.BOOKED, null);
            bookings.save(booking);
            notifier.bookingConfirmed(booking, session);
        } else {
            int nextPosition = (int) bookings.countBySessionIdAndStatus(sessionId, BookingStatus.WAITLIST) + 1;
            booking = new Booking(tenantId, sessionId, userId, BookingStatus.WAITLIST, nextPosition);
            bookings.save(booking);
            notifier.waitlisted(booking, session);
        }
        return booking;
    }

    /** El propio socio cancela su reserva: exige que sea suya y respeta el límite de 2 horas. */
    @Transactional
    public void cancel(UUID tenantId, UUID userId, UUID bookingId) {
        Booking booking = bookings.findByIdAndTenantId(bookingId, tenantId)
                .filter(b -> b.getUserId().equals(userId))
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        ClassSession session = sessions.lockById(tenantId, booking.getSessionId())
                .orElseThrow(() -> new SessionNotFoundException(booking.getSessionId()));

        ensureCancellable(booking);

        Instant limit = session.getStartsAt().minus(CANCELLATION_WINDOW_HOURS, ChronoUnit.HOURS);
        if (!Instant.now().isBefore(limit)) {
            throw new CancellationWindowClosedException();
        }

        performCancel(booking, session);
    }

    /**
     * Staff/owner cancela la reserva de cualquier socio, sin el límite de 2 horas (LOGIC.md §5.3:
     * "Staff/Owner pueden cancelar cuando quieran"). No comprueba de quién es la reserva: eso lo
     * hace el filtro de rol en {@code SecurityConfig}, no este método.
     */
    @Transactional
    public void cancelAsStaff(UUID tenantId, UUID bookingId) {
        Booking booking = bookings.findByIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        ClassSession session = sessions.lockById(tenantId, booking.getSessionId())
                .orElseThrow(() -> new SessionNotFoundException(booking.getSessionId()));

        ensureCancellable(booking);
        performCancel(booking, session);
    }

    /**
     * Staff/owner cancela la sesión completa: todas las reservas activas pasan a CANCELLED y se
     * avisa a los socios afectados (LOGIC.md §5.5). Devuelve cuántas reservas se vieron afectadas.
     */
    @Transactional
    public int cancelSession(UUID tenantId, UUID sessionId) {
        ClassSession session = sessions.lockById(tenantId, sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            return 0;
        }

        session.cancel();
        sessions.save(session);

        var activeBookings = bookings.findActiveBySessionId(sessionId);
        activeBookings.forEach(Booking::cancel);
        bookings.saveAll(activeBookings);
        notifier.sessionCancelled(session, activeBookings.size());

        return activeBookings.size();
    }

    private void ensureCancellable(Booking booking) {
        if (booking.getStatus() != BookingStatus.BOOKED && booking.getStatus() != BookingStatus.WAITLIST) {
            throw new BookingNotAllowedException("Esta reserva ya no está activa.");
        }
    }

    private void performCancel(Booking booking, ClassSession session) {
        BookingStatus previousStatus = booking.getStatus();
        Integer previousPosition = booking.getWaitlistPosition();
        booking.cancel();
        bookings.save(booking);
        notifier.bookingCancelled(booking, session);

        if (previousStatus == BookingStatus.BOOKED) {
            promoteNextFromWaitlist(session);
        } else if (previousPosition != null) {
            shiftWaitlistUp(session.getId(), previousPosition);
        }
    }

    private void promoteNextFromWaitlist(ClassSession session) {
        bookings.findFirstBySessionIdAndStatusOrderByWaitlistPositionAsc(session.getId(), BookingStatus.WAITLIST)
                .ifPresent(next -> {
                    Integer promotedPosition = next.getWaitlistPosition();
                    next.promoteToBooked();
                    bookings.save(next);
                    notifier.promotedFromWaitlist(next, session);
                    if (promotedPosition != null) {
                        shiftWaitlistUp(session.getId(), promotedPosition);
                    }
                });
    }

    /** Cierra el hueco en la lista de espera tras quitar a alguien de la posición indicada. */
    private void shiftWaitlistUp(UUID sessionId, int removedPosition) {
        var rest = bookings.findBySessionIdAndStatusAndWaitlistPositionGreaterThanOrderByWaitlistPositionAsc(
                sessionId, BookingStatus.WAITLIST, removedPosition);
        rest.forEach(Booking::decrementWaitlistPosition);
        bookings.saveAll(rest);
    }
}
