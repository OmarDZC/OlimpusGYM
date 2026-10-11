package com.gymflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Trozo 8a-2: solo funciona con el perfil "demo" activo (ver {@code AuthController}). */
public record DemoLoginRequest(
    @NotBlank @Pattern(regexp = "MEMBER|OWNER", message = "Tipo de cuenta de demo no válido") String role) {
}
