package com.gymflow.booking;

import java.util.UUID;

/** También se usa para "no es tuya": no revelamos si la reserva existe de otro socio. */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID bookingId) {
        super("No existe la reserva " + bookingId);
    }
}
