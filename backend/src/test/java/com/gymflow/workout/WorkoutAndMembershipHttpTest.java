package com.gymflow.workout;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.gymflow.workout.dto.LogSetRequest;
import com.gymflow.workout.dto.PlanDay;
import com.gymflow.workout.dto.PlanExercise;
import com.gymflow.workout.dto.WeeklyPlan;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
 * Mi cuenta (Trozo 3): cuota, rutina de hoy, constancia y marcar series. Cubre sobre todo
 * el aislamiento entre socios (LOGIC.md §2: "un usuario no puede ver ni modificar... datos
 * de otro usuario"). Se omite automáticamente si no hay Docker disponible.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class WorkoutAndMembershipHttpTest {

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
    WorkoutPlanRepository workoutPlans;

    @Autowired
    WorkoutLogRepository workoutLogs;

    @Autowired
    WorkoutService workoutService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private Plan plan;
    private int todayIndex;

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        plan = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        todayIndex = LocalDate.now(ZoneId.of("Europe/Madrid")).getDayOfWeek().getValue() - 1;
    }

    private User createMemberWithMembership(String email) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Socio Prueba", Role.MEMBER);
        users.save(user);
        memberships.save(new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));
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

    private WorkoutPlan createPlanForUser(User user, String exerciseName) {
        WeeklyPlan weeklyPlan = new WeeklyPlan(List.of(
            new PlanDay(todayIndex, "Día de prueba", "Fuerza", false, List.of(
                new PlanExercise(0, exerciseName, 3, "10", 60.0, 90, null)))));
        LocalDate weekStart = LocalDate.now(ZoneId.of("Europe/Madrid")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WorkoutPlan workoutPlan = new WorkoutPlan(
            tenant.getId(), user.getId(), weekStart, "Fuerza", "Intermedio", 1,
            workoutService.serialize(weeklyPlan), PlanSource.MANUAL);
        return workoutPlans.save(workoutPlan);
    }

    @Test
    void memberSeesOwnTodayWorkout() throws Exception {
        User user = createMemberWithMembership("rutina.propia@test.local");
        createPlanForUser(user, "Sentadilla");
        String token = loginAndGetToken("rutina.propia@test.local");

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(true))
            .andExpect(jsonPath("$.exercises[0].name").value("Sentadilla"));
    }

    @Test
    void memberWithoutPlanGetsHasPlanFalse() throws Exception {
        createMemberWithMembership("sin.rutina@test.local");
        String token = loginAndGetToken("sin.rutina@test.local");

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(false));
    }

    @Test
    void memberNeverSeesAnotherMembersWorkout() throws Exception {
        User userA = createMemberWithMembership("socio.a@test.local");
        createMemberWithMembership("socio.b@test.local");
        createPlanForUser(userA, "Press banca"); // solo A tiene rutina hoy

        String tokenB = loginAndGetToken("socio.b@test.local");

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(false));
    }

    @Test
    void loggingASetPersistsAndShowsAsDoneAfterward() throws Exception {
        User user = createMemberWithMembership("marca.serie@test.local");
        createPlanForUser(user, "Peso muerto");
        String token = loginAndGetToken("marca.serie@test.local");

        String body = objectMapper.writeValueAsString(new LogSetRequest(todayIndex, 0, 0, true, 65.0, 10));
        mvc.perform(post("/api/v1/me/workout/logs").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exercises[0].sets[0].done").value(true))
            .andExpect(jsonPath("$.exercises[0].sets[1].done").value(false));
    }

    @Test
    void memberWithoutOwnPlanCannotLogSets_andNeverTouchesAnothersPlan() throws Exception {
        User userA = createMemberWithMembership("plan.ajeno.a@test.local");
        createMemberWithMembership("plan.ajeno.b@test.local");
        WorkoutPlan planA = createPlanForUser(userA, "Remo");

        String tokenB = loginAndGetToken("plan.ajeno.b@test.local");
        String body = objectMapper.writeValueAsString(new LogSetRequest(todayIndex, 0, 0, true, null, null));
        mvc.perform(post("/api/v1/me/workout/logs").header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        assertTrue(workoutLogs.findByWorkoutPlanId(planA.getId()).isEmpty(), "B no debe poder escribir en el plan de A");
    }

    @Test
    void membershipReturnsOwnPlanDetails() throws Exception {
        createMemberWithMembership("cuota.a@test.local");
        String tokenA = loginAndGetToken("cuota.a@test.local");

        mvc.perform(get("/api/v1/me/membership").header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planCode").value("COMPLETA"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void streakCountsLoggedDaysForThatUserOnly() throws Exception {
        User user = createMemberWithMembership("constancia@test.local");
        WorkoutPlan userPlan = createPlanForUser(user, "Zancadas");
        workoutLogs.save(new WorkoutLog(tenant.getId(), user.getId(), userPlan.getId(), todayIndex, 0, 0, true, null, null));

        String token = loginAndGetToken("constancia@test.local");

        mvc.perform(get("/api/v1/me/streak").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(true))
            .andExpect(jsonPath("$.targetDays").value(1))
            .andExpect(jsonPath("$.completedDays").value(1));
    }

    @Test
    void workoutEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/me/workout/today")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me/streak")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me/membership")).andExpect(status().isForbidden());
    }
}
