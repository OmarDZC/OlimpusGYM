package com.gymflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * En producción y en la demo pública, firmar JWT con un secreto corto, vacío o el valor de
 * ejemplo del propio repositorio (público en el historial de git) equivale a no tener secreto:
 * cualquiera podría falsificar un token válido. La demo es tan pública como producción (mismo
 * `docker-compose.prod.yml`, mismo dominio expuesto), así que le aplica igual. Mejor que el
 * backend ni arranque a que arranque inseguro.
 *
 * <p>Atado a los perfiles "prod" y "demo" en concreto, listados uno a uno, no a "ni dev ni
 * demo": la mayoría de los tests con Testcontainers del backend no activan ningún perfil (ni
 * "dev" ni "demo" ni "prod"), y con una expresión "!dev &amp; !demo" este validador se colaba
 * también ahí, rompiéndolos todos porque `application.yml` no trae un `GYMFLOW_JWT_SECRET` real
 * por defecto (descubierto en la CI real, que sí tiene Docker para Testcontainers, a diferencia
 * de esta máquina). Una lista explícita no tiene ese problema: "ningún perfil" no es "prod" ni
 * "demo".
 */
@Component
@Profile({"prod", "demo"})
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
