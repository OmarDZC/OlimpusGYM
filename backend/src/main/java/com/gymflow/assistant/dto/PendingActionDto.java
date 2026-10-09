package com.gymflow.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** Propuesta de reservar o cancelar, pendiente de que el socio la confirme en una petición aparte. */
public record PendingActionDto(
    @NotBlank @Pattern(regexp = "book|cancel", message = "Tipo de acción no válido") String type,
    UUID sessionId,
    UUID bookingId,
    String summary) {
}
