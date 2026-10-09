package com.gymflow.assistant.ai;

/** El servicio de IA respondió pero no logró completar la conversación. El cliente ve un 502. */
public class AssistantFailedException extends RuntimeException {

    public AssistantFailedException() {
        super("No se pudo completar la conversación esta vez. Inténtalo de nuevo.");
    }
}
