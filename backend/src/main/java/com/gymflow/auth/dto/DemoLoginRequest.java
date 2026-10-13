package com.gymflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Solo funciona con el perfil "demo" activo (ver {@code AuthController}). El OWNER real de
 * la demo nunca es una opción aquí (Trozo 9f): solo las cuentas públicas de staff/socio. */
public record DemoLoginRequest(
    @NotBlank @Pattern(regexp = "MEMBER|STAFF", message = "Tipo de cuenta de demo no válido") String role) {
}
