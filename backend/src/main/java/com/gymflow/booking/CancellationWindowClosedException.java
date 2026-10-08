package com.gymflow.booking;

/** LOGIC.md §5.3: el socio solo puede cancelar hasta 2 h antes del inicio de la sesión. */
public class CancellationWindowClosedException extends BookingNotAllowedException {

    public CancellationWindowClosedException() {
        super("Ya no puedes cancelar esta reserva: faltan menos de 2 horas para que empiece la clase.");
    }
}
