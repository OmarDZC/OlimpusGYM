package com.gymflow.admin;

import org.springframework.security.core.Authentication;

/** Pequeña ayuda compartida: qué puede ver/hacer cada rol dentro del panel de gestión. */
final class AdminAuth {

    private AdminAuth() {
    }

    static boolean isOwner(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
    }
}
