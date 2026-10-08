package com.gymflow.workout;

/** Sin cuota activa, o la tarifa no incluye entrenador con IA (LOGIC.md §3 y §6). 403 para el cliente. */
public class WorkoutNotAllowedException extends RuntimeException {

    public WorkoutNotAllowedException(String message) {
        super(message);
    }
}
