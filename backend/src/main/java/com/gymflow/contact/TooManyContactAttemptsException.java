package com.gymflow.contact;

/** Límite de envíos por IP superado (Trozo 7a, LOGIC.md §6: "límite de envíos por IP para evitar abuso"). */
public class TooManyContactAttemptsException extends RuntimeException {

    public TooManyContactAttemptsException() {
        super("Has enviado demasiados mensajes seguidos. Espera unos minutos e inténtalo de nuevo.");
    }
}
