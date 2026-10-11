package com.gymflow.workout.dto;

import java.util.List;

/** Un día de la rutina semanal vigente, con el progreso real de cada serie. */
public record DayProgress(int dayIndex, String label, String focus, boolean restDay, List<ExerciseProgress> exercises) {
}
