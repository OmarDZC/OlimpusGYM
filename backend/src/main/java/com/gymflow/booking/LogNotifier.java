package com.gymflow.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LogNotifier.class);

    @Override
    public void bookingConfirmed(Booking booking, ClassSession session) {
        log.info(
                "[notificacion] Reserva confirmada: usuario={} sesion='{}' ({}) inicio={}",
                booking.getUserId(), session.getName(), session.getId(), session.getStartsAt());
    }

    @Override
    public void waitlisted(Booking booking, ClassSession session) {
        log.info(
                "[notificacion] En lista de espera (posicion {}): usuario={} sesion='{}' ({})",
                booking.getWaitlistPosition(), booking.getUserId(), session.getName(), session.getId());
    }

    @Override
    public void promotedFromWaitlist(Booking booking, ClassSession session) {
        log.info(
                "[notificacion] Promovido desde lista de espera a plaza confirmada: usuario={} sesion='{}' ({})",
                booking.getUserId(), session.getName(), session.getId());
    }

    @Override
    public void bookingCancelled(Booking booking, ClassSession session) {
        log.info(
                "[notificacion] Reserva cancelada: usuario={} sesion='{}' ({})",
                booking.getUserId(), session.getName(), session.getId());
    }

    @Override
    public void sessionCancelled(ClassSession session, int affectedBookings) {
        log.info(
                "[notificacion] Sesion cancelada por el gimnasio: '{}' ({}), avisadas {} reservas activas",
                session.getName(), session.getId(), affectedBookings);
    }
}
