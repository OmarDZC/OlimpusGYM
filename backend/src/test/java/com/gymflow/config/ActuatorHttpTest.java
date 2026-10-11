package com.gymflow.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Trozo 8a-2: solo salud y métricas básicas de Actuator, accesibles sin autenticación (nunca
 * públicas de verdad: Caddy no las expone, ver Caddyfile); nada más de Actuator debe estar
 * disponible. Se omite automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHttpTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsReachableWithoutAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void metricsIsReachableWithoutAuthentication() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isOk());
    }

    @Test
    void anythingElseInActuatorIsNotExposed() throws Exception {
        // No están en la lista de permitAll de SecurityConfig: el filtro de seguridad corta
        // antes incluso de que Spring compruebe si el endpoint existe, así que da 403 (sin
        // autenticación), no 404 (aunque tampoco estén expuestos por management.endpoints).
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
    }
}
