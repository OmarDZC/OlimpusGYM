package com.gymflow.admin;

import com.gymflow.admin.dto.CreateSessionRequest;
import com.gymflow.admin.dto.UpdateSessionRequest;
import com.gymflow.auth.UserRepository;
import com.gymflow.booking.BookingRepository;
import com.gymflow.booking.BookingService;
import com.gymflow.booking.BookingStatus;
import com.gymflow.booking.ClassSession;
import com.gymflow.booking.ClassSessionRepository;
import com.gymflow.booking.SessionNotFoundException;
import com.gymflow.booking.dto.SessionSummary;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crear, editar y cancelar sesiones de clase (LOGIC.md §6, solo OWNER/STAFF). */
@Service
public class AdminSessionService {

    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final BookingService bookingService;

    public AdminSessionService(
            ClassSessionRepository sessions,
            BookingRepository bookings,
            UserRepository users,
            BookingService bookingService) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.users = users;
        this.bookingService = bookingService;
    }

    @Transactional
    public List<SessionSummary> create(UUID tenantId, CreateSessionRequest request) {
        if (request.coachUserId() != null) {
            users.findById(request.coachUserId())
                    .filter(u -> u.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new IllegalArgumentException("El entrenador indicado no existe en este gimnasio"));
        }

        int weeks = request.repeatWeeks() != null ? request.repeatWeeks() : 1;
        List<ClassSession> created = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            long offsetDays = 7L * i;
            ClassSession session = new ClassSession(
                    tenantId,
                    request.name(),
                    request.discipline(),
                    request.description(),
                    request.coachUserId(),
                    request.room(),
                    request.startsAt().plus(offsetDays, ChronoUnit.DAYS),
                    request.endsAt().plus(offsetDays, ChronoUnit.DAYS),
                    request.capacity());
            created.add(sessions.save(session));
        }
        return created.stream().map(this::toSummary).toList();
    }

    @Transactional
    public SessionSummary update(UUID tenantId, UUID sessionId, UpdateSessionRequest request) {
        ClassSession session = sessions.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (request.coachUserId() != null) {
            users.findById(request.coachUserId())
                    .filter(u -> u.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new IllegalArgumentException("El entrenador indicado no existe en este gimnasio"));
        }
        session.applyUpdate(
                request.name(), request.discipline(), request.description(), request.coachUserId(), request.room(),
                request.startsAt(), request.endsAt(), request.capacity());
        return toSummary(sessions.save(session));
    }

    /** Cancela la sesión completa: todas las reservas activas pasan a CANCELLED (LOGIC.md §5.5). */
    @Transactional
    public int cancel(UUID tenantId, UUID sessionId) {
        return bookingService.cancelSession(tenantId, sessionId);
    }

    private SessionSummary toSummary(ClassSession session) {
        long booked = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.BOOKED);
        long waitlisted = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.WAITLIST);
        int freeSlots = (int) Math.max(0, session.getCapacity() - booked);
        String coachName = session.getCoachUserId() != null
                ? users.findById(session.getCoachUserId()).map(com.gymflow.auth.User::getFullName).orElse(null)
                : null;
        return new SessionSummary(
                session.getId(), session.getName(), session.getDiscipline(), session.getRoom(), coachName,
                session.getStartsAt(), session.getEndsAt(), session.getCapacity(), (int) booked, freeSlots,
                (int) waitlisted, session.getStatus().name());
    }
}
