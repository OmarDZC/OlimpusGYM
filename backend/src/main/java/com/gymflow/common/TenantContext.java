package com.gymflow.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Guarda el tenant (gimnasio) de la petición actual en un ThreadLocal.
 * Fase 1: se rellenará desde el JWT en lugar de la cabecera X-Tenant-Id.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No hay tenant en el contexto de la petición"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
