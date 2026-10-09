package com.gymflow.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arreglo: "Tu próxima clase" enseñaba una clase ya pasada (bookings[0] sin filtrar por hora de
 * inicio, ordenado por fecha de creación de la reserva en vez de por el inicio de la clase).
 * GET /me/bookings ahora solo trae clases futuras, ordenadas por la más próxima primero;
 * GET /me/bookings/history trae las pasadas, la más reciente primero. Con reloj controlado
 * (@MockitoBean Clock) para que "ayer"/"dentro de 1h"/"dentro de 3h" sean deterministas.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MyBookingsHistoryHttpTest {

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
    UserRepository users;

    @Autowired
    PlanRepository plans;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    ClassSessionRepository sessions;

    @Autowired
    BookingRepository bookings;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    Clock clock;

    private Tenant tenant;
    private Plan plan;
    private static final Instant REFERENCE_NOW = Instant.parse("2026-10-09T12:00:00Z");

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        plan = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        Mockito.when(clock.instant()).thenReturn(REFERENCE_NOW);
    }

    private User createMemberWithMembership(String email) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Socio Prueba", Role.MEMBER);
        users.save(user);
        memberships.save(new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));
        return user;
    }

    private String doLogin(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, "ClaveDePrueba123!"));
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private ClassSession createSession(String name, Instant startsAt) {
        ClassSession session = new ClassSession(
                tenant.getId(), name, "CrossFit", null, null, "Sala 1",
                startsAt, startsAt.plus(45, ChronoUnit.MINUTES), 10);
        return sessions.save(session);
    }

    private Booking createActiveBooking(ClassSession session, User member) {
        return bookings.save(new Booking(tenant.getId(), session.getId(), member.getId(), BookingStatus.BOOKED, null));
    }

    @Test
    void nextClassAndActiveListOnlyShowFutureBookings_sortedBySoonestFirst() throws Exception {
        User member = createMemberWithMembership("proxima.clase@test.local");
        ClassSession yesterday = createSession("Clase de ayer", REFERENCE_NOW.minus(1, ChronoUnit.DAYS));
        ClassSession inThreeHours = createSession("Clase en 3 horas", REFERENCE_NOW.plus(3, ChronoUnit.HOURS));
        ClassSession inOneHour = createSession("Clase en 1 hora", REFERENCE_NOW.plus(1, ChronoUnit.HOURS));
        createActiveBooking(yesterday, member);
        createActiveBooking(inThreeHours, member);
        createActiveBooking(inOneHour, member);

        String token = doLogin("proxima.clase@test.local");

        mvc.perform(get("/api/v1/me/bookings").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sessionName").value("Clase en 1 hora"))
            .andExpect(jsonPath("$[1].sessionName").value("Clase en 3 horas"));
    }

    @Test
    void historyShowsPastBookingsMostRecentFirst_andNeverFutureOnes() throws Exception {
        User member = createMemberWithMembership("historial@test.local");
        ClassSession twoDaysAgo = createSession("Clase de hace 2 días", REFERENCE_NOW.minus(2, ChronoUnit.DAYS));
        ClassSession yesterday = createSession("Clase de ayer", REFERENCE_NOW.minus(1, ChronoUnit.DAYS));
        ClassSession inOneHour = createSession("Clase en 1 hora", REFERENCE_NOW.plus(1, ChronoUnit.HOURS));
        createActiveBooking(twoDaysAgo, member);
        createActiveBooking(yesterday, member);
        createActiveBooking(inOneHour, member);

        String token = doLogin("historial@test.local");

        mvc.perform(get("/api/v1/me/bookings/history").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sessionName").value("Clase de ayer"))
            .andExpect(jsonPath("$[1].sessionName").value("Clase de hace 2 días"));
    }

    @Test
    void memberWithOnlyAPastBookingSeesNoNextClass() throws Exception {
        User member = createMemberWithMembership("solo.pasado@test.local");
        ClassSession yesterday = createSession("Clase de ayer", REFERENCE_NOW.minus(1, ChronoUnit.DAYS));
        createActiveBooking(yesterday, member);

        String token = doLogin("solo.pasado@test.local");

        mvc.perform(get("/api/v1/me/bookings").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
