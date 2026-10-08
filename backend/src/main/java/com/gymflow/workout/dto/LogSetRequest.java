package com.gymflow.workout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LogSetRequest(
    @NotNull @Min(0) @Max(6) Integer dayIndex,
    @NotNull @Min(0) Integer exerciseIndex,
    @NotNull @Min(0) Integer setIndex,
    @NotNull Boolean done,
    Double weightKg,
    Integer reps) {
}
