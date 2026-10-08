package com.gymflow.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateSessionRequest(
    @NotBlank String name,
    @NotBlank String discipline,
    String description,
    UUID coachUserId,
    @NotBlank String room,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Min(1) int capacity,
    /** Repetición semanal (LOGIC.md §6): crea esta sesión N semanas seguidas. Null o 1 = solo una. */
    @Min(1) @Max(12) Integer repeatWeeks) {
}
