package com.gymflow.booking;

import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.booking.dto.MyBookingResponse;
import com.gymflow.booking.dto.SessionSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta las respuestas de horario y "mis reservas" a partir de las entidades planas. */
@Service
public class ScheduleService {

    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final Clock clock;

    public ScheduleService(
            ClassSessionRepository sessions, BookingRepository bookings, UserRepository users, Clock clock) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> listSchedule(
            UUID tenantId, Instant from, Instant to, String discipline, UUID coachId, String room) {
        List<ClassSession> found = sessions.search(tenantId, from, to, discipline, coachId, room);
        Map<UUID, String> coachNames = coachNames(found.stream().map(ClassSession::getCoachUserId).toList());

        return found.stream()
                .map(session -> {
                    long booked = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.BOOKED);
                    long waitlisted = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.WAITLIST);
                    int freeSlots = (int) Math.max(0, session.getCapacity() - booked);
                    return new SessionSummary(
                            session.getId(),
                            session.getName(),
                            session.getDiscipline(),
                            session.getRoom(),
                            coachNames.get(session.getCoachUserId()),
                            session.getStartsAt(),
                            session.getEndsAt(),
                            session.getCapacity(),
                            (int) booked,
                            freeSlots,
                            (int) waitlisted,
                            session.getStatus().name());
                })
                .toList();
    }

    /** Reservas activas (BOOKED/WAITLIST) de clases que todavía no han empezado, la más próxima primero. */
    @Transactional(readOnly = true)
    public List<MyBookingResponse> listUpcomingForUser(UUID tenantId, UUID userId) {
        Instant now = clock.instant();
        return activeBookingResponses(tenantId, userId).stream()
                .filter(r -> r.startsAt().isAfter(now))
                .sorted(Comparator.comparing(MyBookingResponse::startsAt))
                .toList();
    }

    /**
     * Reservas activas (BOOKED/WAITLIST) de clases que ya han empezado: el socio nunca las
     * canceló a tiempo ni staff canceló la sesión, así que siguen "activas" en la base de
     * datos aunque ya hayan pasado. Se muestran como historial, la más reciente primero.
     */
    @Transactional(readOnly = true)
    public List<MyBookingResponse> listHistoryForUser(UUID tenantId, UUID userId) {
        Instant now = clock.instant();
        return activeBookingResponses(tenantId, userId).stream()
                .filter(r -> !r.startsAt().isAfter(now))
                .sorted(Comparator.comparing(MyBookingResponse::startsAt).reversed())
                .toList();
    }

    private List<MyBookingResponse> activeBookingResponses(UUID tenantId, UUID userId) {
        List<Booking> active = bookings.findActiveForUser(tenantId, userId);
        Map<UUID, ClassSession> sessionById = new HashMap<>();
        active.forEach(b -> sessions.findByIdAndTenantId(b.getSessionId(), tenantId).ifPresent(s -> sessionById.put(s.getId(), s)));
        Map<UUID, String> coachNames = coachNames(sessionById.values().stream().map(ClassSession::getCoachUserId).toList());

        return active.stream()
                .map(b -> {
                    ClassSession session = sessionById.get(b.getSessionId());
                    if (session == null) {
                        return null;
                    }
                    return new MyBookingResponse(
                            b.getId(),
                            session.getId(),
                            session.getName(),
                            session.getDiscipline(),
                            session.getRoom(),
                            coachNames.get(session.getCoachUserId()),
                            session.getStartsAt(),
                            session.getEndsAt(),
                            b.getStatus().name(),
                            b.getWaitlistPosition());
                })
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Devuelve siempre un {@link HashMap} normal, nunca {@code Map.of()}: una sesión sin
     * entrenador tiene {@code coachUserId == null}, y el mapa inmutable de {@code Map.of()}
     * lanza NullPointerException al consultarlo con una clave null (lo hacía incluso estando
     * vacío). HashMap sí admite null como clave y devuelve null sin más.
     */
    private Map<UUID, String> coachNames(List<UUID> coachIds) {
        List<UUID> distinctIds = coachIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, String> result = new HashMap<>();
        for (User user : users.findAllById(distinctIds)) {
            result.put(user.getId(), user.getFullName());
        }
        return result;
    }
}
