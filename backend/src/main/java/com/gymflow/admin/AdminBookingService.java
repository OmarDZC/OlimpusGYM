package com.gymflow.admin;

import com.gymflow.admin.dto.AdminBookingSummary;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.booking.Booking;
import com.gymflow.booking.BookingRepository;
import com.gymflow.booking.BookingService;
import com.gymflow.booking.ClassSession;
import com.gymflow.booking.ClassSessionRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listado y cancelación de reservas desde el panel de gestión (LOGIC.md §6). */
@Service
public class AdminBookingService {

    private final BookingRepository bookings;
    private final ClassSessionRepository sessions;
    private final UserRepository users;
    private final BookingService bookingService;

    public AdminBookingService(
            BookingRepository bookings,
            ClassSessionRepository sessions,
            UserRepository users,
            BookingService bookingService) {
        this.bookings = bookings;
        this.sessions = sessions;
        this.users = users;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingSummary> list(UUID tenantId, Instant from, Instant to) {
        List<Booking> found = bookings.findByTenantAndSessionWindow(tenantId, from, to);
        Map<UUID, ClassSession> sessionById = new HashMap<>();
        Map<UUID, User> userById = new HashMap<>();

        return found.stream()
                .map(b -> {
                    ClassSession session = sessionById.computeIfAbsent(
                            b.getSessionId(), id -> sessions.findByIdAndTenantId(id, tenantId).orElse(null));
                    User member = userById.computeIfAbsent(b.getUserId(), id -> users.findById(id).orElse(null));
                    if (session == null || member == null) {
                        return null;
                    }
                    return new AdminBookingSummary(
                            b.getId(), member.getId(), member.getFullName(), session.getName(),
                            session.getStartsAt(), b.getStatus().name(), b.getWaitlistPosition());
                })
                .filter(s -> s != null)
                .toList();
    }

    /** Staff/owner cancela la reserva de cualquier socio, sin el límite de 2 horas (LOGIC.md §5.3). */
    public void cancel(UUID tenantId, UUID bookingId) {
        bookingService.cancelAsStaff(tenantId, bookingId);
    }
}
