package com.gymflow.workout.ai;

/** El servicio de IA no respondió (caído, sin LLM configurado, timeout...). El cliente ve un 503. */
public class TrainerUnavailableException extends RuntimeException {

    public TrainerUnavailableException() {
        super("El entrenador con IA no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.");
    }
}
