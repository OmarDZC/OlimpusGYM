package com.gymflow.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
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
 * Franja "En directo" de la portada (Trozo 7b, LOGIC.md §9): solo datos calculados de verdad,
 * nunca inventados. Reloj controlado (@MockitoBean Clock) para los casos límite. Se omite
 * automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PublicLiveHttpTest {

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

    @Autowired
    BookingRepository bookings;

    @Autowired
    UserRepository users;

    @MockitoBean
    Clock clock;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        // Aislamiento entre tests: todos comparten la misma base de datos de Testcontainers (sin
        // rollback por test) y varios usan fechas cercanas al 9 de octubre de 2026, así que una
        // sesión de un test contaminaría la ventana "hoy" del siguiente si no se limpia antes.
        bookings.deleteAll();
        sessions.deleteAll();
    }

    private ClassSession createSession(String name, Instant startsAt, int capacity) {
        ClassSession session = new ClassSession(
                tenant.getId(), name, "CrossFit", null, null, "Sala 1",
                startsAt, startsAt.plus(45, ChronoUnit.MINUTES), capacity);
        return sessions.save(session);
    }

    private void bookSeats(ClassSession session, int count) {
        for (int i = 0; i < count; i++) {
            User member = users.save(new User(
                    tenant.getId(), "socia" + i + "." + session.getId() + "@test.local",
                    "{noop}x", "Socia Prueba", Role.MEMBER));
            bookings.save(new Booking(tenant.getId(), session.getId(), member.getId(), BookingStatus.BOOKED, null));
        }
    }

    @Test
    void withNoSessionsAtAll_returnsAllZerosAndNoNextClass() throws Exception {
        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-10-09T12:00:00Z"));

        mvc.perform(get("/api/v1/public/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesRemainingToday").value(0))
                .andExpect(jsonPath("$.nextClass").doesNotExist())
                .andExpect(jsonPath("$.classesWithFreeSlotsNow").value(0));
    }

    @Test
    void ignoresSessionsThatAlreadyStarted() throws Exception {
        Instant now = Instant.parse("2026-10-09T12:00:00Z");
        Mockito.when(clock.instant()).thenReturn(now);
        createSession("Clase de esta mañana (ya empezó)", now.minus(3, ChronoUnit.HOURS), 10);

        mvc.perform(get("/api/v1/public/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesRemainingToday").value(0))
                .andExpect(jsonPath("$.nextClass").doesNotExist());
    }

    @Test
    void aFullClassCountsAsRemainingButNotAsHavingFreeSlots() throws Exception {
        Instant now = Instant.parse("2026-10-09T12:00:00Z");
        Mockito.when(clock.instant()).thenReturn(now);
        ClassSession full = createSession("Spinning completa", now.plus(2, ChronoUnit.HOURS), 2);
        bookSeats(full, 2);

        mvc.perform(get("/api/v1/public/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesRemainingToday").value(1))
                .andExpect(jsonPath("$.nextClass.name").value("Spinning completa"))
                .andExpect(jsonPath("$.nextClass.freeSlots").value(0))
                .andExpect(jsonPath("$.classesWithFreeSlotsNow").value(0));
    }

    @Test
    void theNextClassIsTheSoonestOneAndFreeSlotsAreCountedSeparately() throws Exception {
        Instant now = Instant.parse("2026-10-09T12:00:00Z");
        Mockito.when(clock.instant()).thenReturn(now);
        createSession("Yoga", now.plus(4, ChronoUnit.HOURS), 10);
        createSession("CrossFit", now.plus(1, ChronoUnit.HOURS), 10);

        mvc.perform(get("/api/v1/public/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesRemainingToday").value(2))
                .andExpect(jsonPath("$.nextClass.name").value("CrossFit"))
                .andExpect(jsonPath("$.classesWithFreeSlotsNow").value(2));
    }

    /**
     * España está en CEST (UTC+2) en estas fechas de 2026 (el cambio a invierno es el último
     * domingo de octubre). A las 23:00 UTC del día 8 ya es la 01:00 del día 9 en Madrid: el
     * límite de "hoy" tiene que calcularse con esa zona, nunca con medianoche UTC.
     */
    @Test
    void theTodayBoundaryFollowsEuropeMadridNotUtc() throws Exception {
        Instant now = Instant.parse("2026-10-08T23:00:00Z"); // 09/10 01:00 en Madrid
        Mockito.when(clock.instant()).thenReturn(now);

        // Dentro de "hoy" (9 de octubre) en Madrid: 20:00 UTC = 22:00 Madrid, día 9.
        createSession("Dentro de hoy en Madrid", Instant.parse("2026-10-09T20:00:00Z"), 10);
        // Ya es "mañana" en Madrid (01:00 del día 10), aunque siga siendo el 9 en UTC.
        createSession("Ya es mañana en Madrid", Instant.parse("2026-10-09T23:00:00Z"), 10);

        mvc.perform(get("/api/v1/public/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesRemainingToday").value(1))
                .andExpect(jsonPath("$.nextClass.name").value("Dentro de hoy en Madrid"));
    }
}
