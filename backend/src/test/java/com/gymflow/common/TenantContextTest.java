package com.gymflow.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void storesAndReturnsTenant() {
        UUID id = UUID.randomUUID();
        TenantContext.set(id);
        assertEquals(id, TenantContext.require());
    }

    @Test
    void requireFailsWithoutTenant() {
        assertTrue(TenantContext.get().isEmpty());
        assertThrows(IllegalStateException.class, TenantContext::require);
    }
}
