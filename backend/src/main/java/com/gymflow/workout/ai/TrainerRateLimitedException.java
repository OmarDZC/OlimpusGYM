package com.gymflow.workout.ai;

/** El servicio de IA devolvió 429: no es que esté caído, es que se agotó el cupo de
 * peticiones del proveedor por ahora. El cliente ve un 429 con un mensaje distinto. */
public class TrainerRateLimitedException extends RuntimeException {

    public TrainerRateLimitedException(String message) {
        super(message);
    }
}
