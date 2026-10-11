package com.gymflow.seed;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Bloque 3: el seed de dev/demo (cuentas de ejemplo, contraseñas conocidas, el admin de
 * desarrollo) nunca debe arrancar en producción. `@Profile({"dev", "demo"})` en
 * `DevDataSeeder` ya lo garantiza; este test comprueba que de verdad es así con el perfil
 * "prod" activo (no solo que la anotación esté puesta, por si algún día cambia).
 *
 * <p>Con el perfil "prod" también arrancan {@code ProductionSecretsValidator} y
 * {@code ProductionAdminSeeder} (Trozo 8a-1): hace falta darles un secreto y un admin válidos
 * para que el contexto llegue a cargar, aunque este test no verifique nada sobre ellos.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("prod")
class DevDataSeederProductionProfileTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("gymflow.jwt.secret", () -> "test-only-secret-not-for-real-use-1234567890");
        registry.add("gymflow.admin.email", () -> "admin-test@example.com");
        registry.add("gymflow.admin.password", () -> "contraseña-de-prueba-123");
    }

    @Autowired
    ApplicationContext context;

    @Test
    void devDataSeederBeanDoesNotExistWithProductionProfile() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DevDataSeeder.class));
    }
}
