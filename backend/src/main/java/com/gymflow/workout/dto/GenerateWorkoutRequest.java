package com.gymflow.workout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** {@code POST /me/workout/generate} (LOGIC.md §6). Los valores de goal/level son los que acepta el servicio de IA. */
public record GenerateWorkoutRequest(
    @NotBlank @Pattern(regexp = "hypertrophy|strength|fat_loss|general", message = "Objetivo no válido") String goal,
    @NotBlank @Pattern(regexp = "beginner|intermediate|advanced", message = "Nivel no válido") String level,
    @NotNull @Min(1) @Max(7) Integer daysPerWeek,
    List<String> equipment,
    List<String> limitations) {

    public GenerateWorkoutRequest {
        equipment = equipment == null ? List.of() : equipment;
        limitations = limitations == null ? List.of() : limitations;
    }
}
