package com.gymflow.booking;

/** La clase ya ha empezado (o terminado): mensaje distinto al de la ventana de 2 horas. */
public class SessionAlreadyStartedException extends BookingNotAllowedException {

    public SessionAlreadyStartedException() {
        super("Esta clase ya ha empezado, no se puede cancelar.");
    }
}
