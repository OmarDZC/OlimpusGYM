package com.gymflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fuera de dev/demo, firmar JWT con un secreto corto, vacío o el valor de ejemplo del propio
 * repositorio (público en el historial de git) equivale a no tener secreto: cualquiera podría
 * falsificar un token válido. Mejor que el backend ni arranque a que arranque inseguro.
 */
@Component
@Profile("!dev & !demo")
public class ProductionSecretsValidator {

    private static final String EXAMPLE_SECRET = "change-me-change-me-change-me-change-me-32b";
    private static final int MIN_SECRET_LENGTH = 32;

    private final String jwtSecret;

    public ProductionSecretsValidator(@Value("${gymflow.jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "GYMFLOW_JWT_SECRET no está definido. Fuera de dev/demo hace falta un secreto real: "
                    + "genera uno con `openssl rand -base64 48`.");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "GYMFLOW_JWT_SECRET es demasiado corto (" + jwtSecret.length() + " caracteres, "
                    + "mínimo " + MIN_SECRET_LENGTH + "). Genera uno con: openssl rand -base64 48");
        }
        if (jwtSecret.equals(EXAMPLE_SECRET)) {
            throw new IllegalStateException(
                "GYMFLOW_JWT_SECRET sigue siendo el valor de ejemplo del repositorio (público en el "
                    + "historial de git). Genera uno real con: openssl rand -base64 48");
        }
    }
}
