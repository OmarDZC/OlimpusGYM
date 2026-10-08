package com.gymflow.booking;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("No existe la sesión " + sessionId);
    }
}
