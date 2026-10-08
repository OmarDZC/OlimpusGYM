package com.gymflow.workout.dto;

import java.util.List;

public record ExerciseProgress(
    int exerciseIndex,
    String name,
    String reps,
    Double weightKg,
    Integer restSeconds,
    String note,
    List<SetProgress> sets) {
}
