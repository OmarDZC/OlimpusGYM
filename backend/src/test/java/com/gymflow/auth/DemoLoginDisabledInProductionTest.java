package com.gymflow.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Trozo 8a-2: en producción (perfil "prod") el modo demo no existe ni se nota que exista.
 * {@code GET /public/config} marca {@code demoMode=false} y {@code POST /auth/demo-login}
 * responde 404 (no 403: no debe dar ninguna pista de que el endpoint existe). Se omite
 * automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class DemoLoginDisabledInProductionTest {

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
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicConfigReportsDemoModeFalse() throws Exception {
        mvc.perform(get("/api/v1/public/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demoMode").value(false));
    }

    @Test
    void demoLoginDoesNotExistInProduction() throws Exception {
        mvc.perform(post("/api/v1/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemoLoginRequestBody("OWNER"))))
            .andExpect(status().isNotFound());
    }

    private record DemoLoginRequestBody(String role) {
    }
}
