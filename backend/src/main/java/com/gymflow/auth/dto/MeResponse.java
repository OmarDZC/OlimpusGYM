package com.gymflow.auth.dto;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import java.util.UUID;

public record MeResponse(UUID userId, UUID tenantId, String email, String fullName, Role role) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
