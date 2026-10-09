package com.gymflow.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code website}: campo trampa contra bots (Trozo 7a). Invisible para una persona, oculto con
 * CSS en vez de {@code display:none} (que algunos rastreadores ya ignoran); si llega relleno, se
 * trata como spam, sin más validación encima.
 */
public record ContactRequest(
        @NotBlank(message = "Escribe tu nombre") @Size(max = 120, message = "Nombre demasiado largo") String name,
        @NotBlank(message = "Escribe tu correo") @Email(message = "Correo no válido") @Size(max = 180, message = "Correo demasiado largo") String email,
        @NotBlank(message = "Escribe tu mensaje") @Size(max = 2000, message = "Mensaje demasiado largo") String message,
        String website) {
}
