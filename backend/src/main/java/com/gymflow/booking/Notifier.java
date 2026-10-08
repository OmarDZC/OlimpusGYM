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
}
