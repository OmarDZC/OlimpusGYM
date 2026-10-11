package com.gymflow.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Trozo 8a-1: fuera de dev/demo, un JWT_SECRET corto, vacío o el valor de ejemplo del repo
 * debe tumbar el arranque. Test unitario puro (sin Spring ni Testcontainers): el validador no
 * depende de nada más que del propio secreto.
 */
class ProductionSecretsValidatorTest {

    @Test
    void rejectsAMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsValidator("").validate());
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsValidator(null).validate());
    }

    @Test
    void rejectsATooShortSecret() {
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsValidator("demasiado-corto").validate());
    }

    @Test
    void rejectsTheExampleSecretFromTheRepo() {
        assertThrows(
            IllegalStateException.class,
            () -> new ProductionSecretsValidator("change-me-change-me-change-me-change-me-32b").validate());
    }

    @Test
    void acceptsARealLongRandomSecret() {
        String realSecret = "a9f3c7e1b2d4f6a8c0e2b4d6f8a0c2e4b6d8f0a2c4e6b8d0f2a4c6e8b0d2f4a6";
        assertDoesNotThrow(() -> new ProductionSecretsValidator(realSecret).validate());
    }
}
