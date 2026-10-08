package com.gymflow.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookSessionRequest(@NotNull(message = "Elige una sesión") UUID sessionId) {
}
