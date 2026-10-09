package com.gymflow.assistant.ai;

/** El servicio de IA no respondió (caído, sin LLM configurado, timeout...). El cliente ve un 503. */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException() {
        super("El asistente con IA no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.");
    }
}
