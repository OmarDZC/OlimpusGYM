package com.gymflow.assistant;

/** Sin cuota activa, o la tarifa no incluye el asistente con IA (LOGIC.md §3 y §6). 403 para el cliente. */
public class AssistantNotAllowedException extends RuntimeException {

    public AssistantNotAllowedException(String message) {
        super(message);
    }
}
