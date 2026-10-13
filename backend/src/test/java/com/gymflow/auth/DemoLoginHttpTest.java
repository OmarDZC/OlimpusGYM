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
 * Trozo 8a-2: con el perfil "demo" activo, {@code GET /public/config} marca {@code demoMode}
 * y los botones "entrar como demo" funcionan de verdad contra las cuentas de ejemplo que crea
 * {@code DevDataSeeder} en ese perfil. Se omite automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoLoginHttpTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("gymflow.jwt.secret", () -> "test-only-secret-not-for-real-use-1234567890");
        registry.add("gymflow.demo.owner-email", () -> "propietario-de-prueba@olympusgym.test");
        registry.add("gymflow.demo.owner-password", () -> "demo-owner-password-de-prueba");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publicConfigReportsDemoModeTrue() throws Exception {
        mvc.perform(get("/api/v1/public/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demoMode").value(true));
    }

    @Test
    void demoLoginAsStaffReturnsTheStaffDemoAccount() throws Exception {
        mvc.perform(post("/api/v1/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemoLoginRequestBody("STAFF"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("marta@olympusgym.test"))
            .andExpect(jsonPath("$.role").value("STAFF"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void demoLoginAsMemberReturnsTheMemberDemoAccount() throws Exception {
        mvc.perform(post("/api/v1/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemoLoginRequestBody("MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("carlos@olympusgym.test"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void demoLoginRejectsAnInvalidRole() throws Exception {
        mvc.perform(post("/api/v1/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemoLoginRequestBody("ADMIN"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void demoLoginNoLongerAcceptsOwnerAsARole() throws Exception {
        // El OWNER real de la demo (DEMO_OWNER_EMAIL/DEMO_OWNER_PASSWORD) nunca es una opción
        // de demo-login: no aparece en los botones de acceso (Trozo 9f).
        mvc.perform(post("/api/v1/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemoLoginRequestBody("OWNER"))))
            .andExpect(status().isBadRequest());
    }

    private record DemoLoginRequestBody(String role) {
    }
}
