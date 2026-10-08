package com.gymflow.workout.dto;

public record PlanExercise(
    int exerciseIndex, String name, int sets, String reps, Double weightKg, Integer restSeconds, String note) {
}
