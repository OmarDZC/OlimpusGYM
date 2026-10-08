package com.gymflow.auth.dto;

import com.gymflow.plan.PlanCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email,
    @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String fullName,
    @NotNull(message = "Elige una tarifa") PlanCode planCode) {
}
