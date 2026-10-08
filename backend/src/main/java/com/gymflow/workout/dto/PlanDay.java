package com.gymflow.workout.dto;

import java.util.List;

/** {@code dayIndex}: 0 = lunes ... 6 = domingo. */
public record PlanDay(int dayIndex, String label, String focus, boolean restDay, List<PlanExercise> exercises) {
}
