package com.gymflow.auth.dto;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import java.util.UUID;

/** Nunca incluye el hash de la contraseña: se construye siempre a partir del DTO, no de la entidad. */
public record AuthResponse(
    String token, UUID userId, UUID tenantId, String email, String fullName, Role role) {

    public static AuthResponse from(User user, String token) {
        return new AuthResponse(
            token, user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
