package com.gymflow.booking;

/**
 * Notificaciones de reservas (LOGIC.md §8). Única implementación del MVP: escribe en log.
 * Email/SMTP se puede añadir después implementando esta misma interfaz, sin tocar BookingService.
 */
public interface Notifier {

    void bookingConfirmed(Booking booking, ClassSession session);

    void waitlisted(Booking booking, ClassSession session);

    void promotedFromWaitlist(Booking booking, ClassSession session);

    void bookingCancelled(Booking booking, ClassSession session);

    /** Staff/owner cancela toda la sesión (LOGIC.md §5.5): se avisa a todos los socios afectados. */
    void sessionCancelled(ClassSession session, int affectedBookings);
}
