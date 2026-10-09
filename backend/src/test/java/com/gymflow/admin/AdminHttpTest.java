package com.gymflow.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.booking.Booking;
import com.gymflow.booking.BookingRepository;
import com.gymflow.booking.BookingService;
import com.gymflow.booking.BookingStatus;
import com.gymflow.booking.ClassSession;
import com.gymflow.booking.ClassSessionRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Panel de gestión (Trozo 4a): resumen, sesiones, socios, reservas y las dos reglas
 * aplazadas del Trozo 2. Cubre sobre todo permisos (LOGIC.md §2) y aislamiento entre
 * tenants. Se omite automáticamente si no hay Docker disponible.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AdminHttpTest {

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
    BookingService bookingService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private Plan plan;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        tenant = tenants.save(new Tenant("Gimnasio " + unique, "gimnasio-" + unique));
        plan = plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true));
    }

    private User createUser(Tenant t, String email, Role role) {
        User user = new User(t.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Usuario Prueba", role);
        users.save(user);
        if (role == Role.MEMBER) {
            Plan tenantPlan = plans.findByTenantIdAndCode(t.getId(), PlanCode.COMPLETA)
                .orElseGet(() -> plans.save(new Plan(t.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
            memberships.save(new Membership(t.getId(), user.getId(), tenantPlan.getId(), MembershipStatus.ACTIVE, Instant.now()));
        }
        return user;
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, "ClaveDePrueba123!"));
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private ClassSession createSession(Tenant t, Instant startsAt, int capacity) {
        ClassSession session = new ClassSession(
            t.getId(), "Clase de prueba", "Test", null, null, "Sala 1", startsAt,
            startsAt.plus(60, ChronoUnit.MINUTES), capacity);
        return sessions.save(session);
    }

    @Test
    void memberGets403OnAdminSummary() throws Exception {
        createUser(tenant, "socio@test.local", Role.MEMBER);
        String token = loginAndGetToken("socio@test.local");

        mvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void staffGets403OnOwnerOnlyChangePlan() throws Exception {
        createUser(tenant, "staff@test.local", Role.STAFF);
        User member = createUser(tenant, "socio.plan@test.local", Role.MEMBER);
        String staffToken = loginAndGetToken("staff@test.local");

        String body = objectMapper.writeValueAsString(new com.gymflow.admin.dto.ChangePlanRequest(PlanCode.BASICA));
        mvc.perform(patch("/api/v1/admin/members/" + member.getId() + "/plan")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void ownerSchedulesADowngradeForAMember_notAppliedUntilRenewal() throws Exception {
        createUser(tenant, "owner@test.local", Role.OWNER);
        plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Básica", 3900, 4, 7, false, false));
        User member = createUser(tenant, "socio.plan2@test.local", Role.MEMBER); // arranca en COMPLETA (5900)
        String ownerToken = loginAndGetToken("owner@test.local");

        // BASICA es más barata que la tarifa actual del socio: el dueño también se somete a la
        // regla de "bajar siempre se programa", no la aplica al instante (Trozo 9b).
        String body = objectMapper.writeValueAsString(new com.gymflow.admin.dto.ChangePlanRequest(PlanCode.BASICA));
        mvc.perform(patch("/api/v1/admin/members/" + member.getId() + "/plan")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), member.getId()).orElseThrow();
        assertEquals(plan.getId(), membership.getPlanId(), "la tarifa actual no cambia hasta la renovación");
        Plan basica = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA).orElseThrow();
        assertEquals(basica.getId(), membership.getPendingPlanId());
    }

    @Test
    void ownerUpgradesAMemberImmediately() throws Exception {
        createUser(tenant, "owner.sube@test.local", Role.OWNER);
        plans.save(new Plan(tenant.getId(), PlanCode.ATLETA_PRO, "Atleta Pro", 7900, null, 30, true, true));
        User member = createUser(tenant, "socio.plan3@test.local", Role.MEMBER); // arranca en COMPLETA (5900)
        String ownerToken = loginAndGetToken("owner.sube@test.local");

        String body = objectMapper.writeValueAsString(new com.gymflow.admin.dto.ChangePlanRequest(PlanCode.ATLETA_PRO));
        mvc.perform(patch("/api/v1/admin/members/" + member.getId() + "/plan")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planCode").value("ATLETA_PRO"));

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), member.getId()).orElseThrow();
        Plan atletaPro = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.ATLETA_PRO).orElseThrow();
        assertEquals(atletaPro.getId(), membership.getPlanId(), "subir se aplica al instante");
    }

    @Test
    void summaryShowsRevenueOnlyForOwner() throws Exception {
        createUser(tenant, "owner.rev@test.local", Role.OWNER);
        createUser(tenant, "staff.rev@test.local", Role.STAFF);
        createUser(tenant, "socio.rev@test.local", Role.MEMBER);

        String ownerToken = loginAndGetToken("owner.rev@test.local");
        String staffToken = loginAndGetToken("staff.rev@test.local");

        mvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlyRevenueCents").value(5900));

        mvc.perform(get("/api/v1/admin/summary").header("Authorization", "Bearer " + staffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlyRevenueCents").doesNotExist());
    }

    @Test
    void adminMembersNeverLeakAcrossTenants() throws Exception {
        createUser(tenant, "owner.a@test.local", Role.OWNER);
        createUser(tenant, "socio.a@test.local", Role.MEMBER);

        Tenant otherTenant = tenants.save(new Tenant("Otro Gimnasio", "otro-gimnasio-" + UUID.randomUUID()));
        createUser(otherTenant, "socio.b@test.local", Role.MEMBER);

        String ownerTokenA = loginAndGetToken("owner.a@test.local");

        mvc.perform(get("/api/v1/admin/members").header("Authorization", "Bearer " + ownerTokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.email == 'socio.b@test.local')]").isEmpty())
            .andExpect(jsonPath("$[?(@.email == 'socio.a@test.local')]").isNotEmpty());
    }

    @Test
    void creatingAndCancellingASessionNotifiesAllActiveBookings() throws Exception {
        createUser(tenant, "owner.sess@test.local", Role.OWNER);
        User memberA = createUser(tenant, "espera.a@test.local", Role.MEMBER);
        User memberB = createUser(tenant, "espera.b@test.local", Role.MEMBER);
        String ownerToken = loginAndGetToken("owner.sess@test.local");

        ClassSession session = createSession(tenant, Instant.now().plus(1, ChronoUnit.DAYS), 1);
        bookingService.book(tenant.getId(), memberA.getId(), session.getId());
        Booking waitlisted = bookingService.book(tenant.getId(), memberB.getId(), session.getId());
        assertEquals(BookingStatus.WAITLIST, waitlisted.getStatus());

        mvc.perform(post("/api/v1/admin/sessions/" + session.getId() + "/cancel")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelledBookings").value(2));

        assertEquals(BookingStatus.CANCELLED, bookings.findById(waitlisted.getId()).orElseThrow().getStatus());
    }

    @Test
    void listingSessionsDoesNotFailWhenNoSessionHasACoach() throws Exception {
        // Regresión: ScheduleService.listSchedule (la usan tanto /public/schedule como
        // /admin/sessions) lanzaba NullPointerException cuando ninguna sesión del resultado
        // tenía entrenador asignado: el mapa de nombres quedaba como Map.of(), que no admite
        // consultarse con una clave null (coachUserId null), ni siquiera estando vacío.
        createUser(tenant, "owner.coachless@test.local", Role.OWNER);
        String ownerToken = loginAndGetToken("owner.coachless@test.local");

        String body = objectMapper.writeValueAsString(new com.gymflow.admin.dto.CreateSessionRequest(
            "Pilates Reformer", "PilatesRegresion", null, null, "Estudio Zen",
            Instant.now().plus(5, ChronoUnit.DAYS), Instant.now().plus(5, ChronoUnit.DAYS).plusSeconds(3600), 10, null));
        mvc.perform(post("/api/v1/admin/sessions")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/admin/sessions?discipline=PilatesRegresion")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Pilates Reformer"))
            .andExpect(jsonPath("$[0].coachName").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void staffCanCancelAnyBookingWithoutTheTwoHourLimit() throws Exception {
        createUser(tenant, "staff.cancel@test.local", Role.STAFF);
        User member = createUser(tenant, "socio.pronto@test.local", Role.MEMBER);
        String staffToken = loginAndGetToken("staff.cancel@test.local");

        // A 30 minutos: un socio ya no podría cancelar esto él mismo (límite de 2 horas).
        ClassSession soon = createSession(tenant, Instant.now().plus(30, ChronoUnit.MINUTES), 10);
        Booking booking = bookingService.book(tenant.getId(), member.getId(), soon.getId());

        mvc.perform(delete("/api/v1/admin/bookings/" + booking.getId())
                .header("Authorization", "Bearer " + staffToken))
            .andExpect(status().isNoContent());

        assertEquals(BookingStatus.CANCELLED, bookings.findById(booking.getId()).orElseThrow().getStatus());
    }
}
