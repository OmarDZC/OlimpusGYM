package com.gymflow.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageDto(
    @NotBlank @Pattern(regexp = "user|assistant", message = "Rol no válido") String role,
    @NotBlank @Size(max = 2000, message = "Mensaje demasiado largo") String content) {
}
