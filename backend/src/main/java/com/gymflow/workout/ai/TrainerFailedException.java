package com.gymflow.workout.ai;

/** El servicio de IA respondió pero no logró generar un plan válido. El cliente ve un 502. */
public class TrainerFailedException extends RuntimeException {

    public TrainerFailedException() {
        super("No se pudo generar tu rutina esta vez. Inténtalo de nuevo en unos minutos.");
    }
}
