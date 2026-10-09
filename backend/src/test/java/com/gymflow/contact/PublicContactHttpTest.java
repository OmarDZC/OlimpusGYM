package com.gymflow.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.contact.dto.ContactRequest;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Formulario público de contacto (Trozo 7a, LOGIC.md §6): sin sesión, con validación, un campo
 * trampa contra bots y un límite de envíos por IP. Cada test usa una IP distinta
 * ({@link #fromIp}) porque el limitador de envíos es un bean en memoria que vive durante toda la
 * clase de test, no solo durante un método. Se omite automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PublicContactHttpTest {

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

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenantRepository tenants;

    @Autowired
    ContactMessageRepository messages;

    @MockitoBean
    ContactNotifier notifier;

    @MockitoBean
    Clock clock;

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @BeforeEach
    void setUp() {
        tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        messages.deleteAll();
        Mockito.when(clock.instant()).thenReturn(NOW);
        Mockito.reset(notifier);
    }

    private String body(String name, String email, String message, String website) throws Exception {
        return objectMapper.writeValueAsString(new ContactRequest(name, email, message, website));
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void savesTheMessageAndNotifies() throws Exception {
        mvc.perform(post("/api/v1/public/contact")
                        .with(fromIp("203.0.113.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana Pérez", "ana@example.test", "¿Tenéis clases de yoga por la mañana?", null)))
                .andExpect(status().isNoContent());

        List<ContactMessage> saved = messages.findAll();
        assertEquals(1, saved.size());
        assertEquals("Ana Pérez", saved.get(0).getName());
        assertEquals("ana@example.test", saved.get(0).getEmail());
        Mockito.verify(notifier).received(Mockito.any());
    }

    @Test
    void rejectsAnInvalidEmail() throws Exception {
        mvc.perform(post("/api/v1/public/contact")
                        .with(fromIp("203.0.113.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana Pérez", "no-es-un-correo", "Hola", null)))
                .andExpect(status().isBadRequest());

        assertTrue(messages.findAll().isEmpty());
    }

    @Test
    void rejectsWhenTheHoneypotFieldIsFilled() throws Exception {
        mvc.perform(post("/api/v1/public/contact")
                        .with(fromIp("203.0.113.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Bot", "bot@example.test", "Spam", "http://spam.example")))
                .andExpect(status().isBadRequest());

        assertTrue(messages.findAll().isEmpty(), "un envío con el campo trampa relleno nunca debe guardarse");
    }

    @Test
    void returns429AfterTooManyAttemptsFromTheSameIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/public/contact")
                            .with(fromIp("203.0.113.4"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Ana Pérez", "ana" + i + "@example.test", "Mensaje " + i, null)))
                    .andExpect(status().isNoContent());
        }

        mvc.perform(post("/api/v1/public/contact")
                        .with(fromIp("203.0.113.4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana Pérez", "ana.otro@example.test", "Uno más", null)))
                .andExpect(status().isTooManyRequests());

        assertEquals(3, messages.findAll().size(), "el cuarto envío no debe haberse guardado");
    }
}
