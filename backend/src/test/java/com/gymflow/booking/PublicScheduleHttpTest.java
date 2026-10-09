package com.gymflow.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arreglo: /horario pide el día de hoy con `from` = medianoche UTC de hoy, así que una clase de
 * esta mañana que ya empezó seguía apareciendo como si se pudiera reservar. El horario público
 * nunca debe devolver sesiones ya empezadas, pase lo que pase en `from` (LOGIC.md §6).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PublicScheduleHttpTest {

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
    TenantRepository tenants;

    @Autowired
    ClassSessionRepository sessions;

    @MockitoBean
    Clock clock;

    private static final Instant REFERENCE_NOW = Instant.parse("2026-10-09T12:00:00Z");

    @BeforeEach
    void setUp() {
        tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        Mockito.when(clock.instant()).thenReturn(REFERENCE_NOW);
    }

    private ClassSession createSession(String name, Instant startsAt) {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG).orElseThrow();
        ClassSession session = new ClassSession(
                tenant.getId(), name, "CrossFit", null, null, "Sala 1",
                startsAt, startsAt.plus(45, ChronoUnit.MINUTES), 10);
        return sessions.save(session);
    }

    @Test
    void neverReturnsASessionThatAlreadyStarted_evenWhenFromIsExplicitlyMidnightToday() throws Exception {
        createSession("Clase de esta mañana (ya empezó)", REFERENCE_NOW.minus(3, ChronoUnit.HOURS));
        createSession("Clase de esta tarde", REFERENCE_NOW.plus(3, ChronoUnit.HOURS));

        // Lo mismo que manda /horario al pedir "hoy": medianoche UTC de hoy como `from`.
        String midnightToday = REFERENCE_NOW.truncatedTo(ChronoUnit.DAYS).toString();
        String endOfToday = REFERENCE_NOW.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS).toString();

        mvc.perform(get("/api/v1/public/schedule").param("from", midnightToday).param("to", endOfToday))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Clase de esta tarde"));
    }
}
