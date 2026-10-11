package com.gymflow.assistant.ai;

/** El servicio de IA devolvió 429: no es que esté caído, es que se agotó el cupo de
 * peticiones del proveedor por ahora. El cliente ve un 429 con un mensaje distinto. */
public class AssistantRateLimitedException extends RuntimeException {

    public AssistantRateLimitedException(String message) {
        super(message);
    }
}
