package com.gymflow.booking;

/** Una regla de negocio de LOGIC.md §5 impide la reserva o la cancelación. Se traduce a 409. */
public class BookingNotAllowedException extends RuntimeException {

    public BookingNotAllowedException(String message) {
        super(message);
    }
}
