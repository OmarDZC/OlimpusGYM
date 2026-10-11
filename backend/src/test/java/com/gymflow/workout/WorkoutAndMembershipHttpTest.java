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
import com.gymflow.workout.ai.AiDay;
import com.gymflow.workout.ai.AiExercise;
import com.gymflow.workout.ai.AiWeeklyPlan;
import com.gymflow.workout.ai.TrainerClient;
import com.gymflow.workout.ai.TrainerUnavailableException;
import com.gymflow.workout.dto.GenerateWorkoutRequest;
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

    @MockitoBean
    TrainerClient trainerClient;

    private Tenant tenant;
    private Plan plan;
    private Plan planWithoutAiTrainer;
    private int todayIndex;

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        plan = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        planWithoutAiTrainer = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Básica", 3900, 4, 7, false, false)));
        todayIndex = LocalDate.now(ZoneId.of("Europe/Madrid")).getDayOfWeek().getValue() - 1;
        Mockito.reset(trainerClient);
    }

    private User createMemberWithMembership(String email) {
        return createMemberWithMembership(email, plan);
    }

    private User createMemberWithMembership(String email, Plan membershipPlan) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Socio Prueba", Role.MEMBER);
        users.save(user);
        memberships.save(new Membership(tenant.getId(), user.getId(), membershipPlan.getId(), MembershipStatus.ACTIVE, Instant.now()));
        return user;
    }

    private static AiWeeklyPlan fakeAiPlan(String summary, int days) {
        List<AiDay> aiDays = java.util.stream.IntStream.rangeClosed(1, days)
            .mapToObj(day -> new AiDay(day, "Enfoque día " + day, List.of(
                new AiExercise("Sentadilla", 3, "8-12", 90, null))))
            .toList();
        return new AiWeeklyPlan(summary, aiDays);
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

    // ---- Rutina semanal completa, "Mi entrenamiento" (Trozo 9d) ----

    private WorkoutPlan createTwoDayPlanForUser(User user) {
        int otherDayIndex = (todayIndex + 2) % 7; // un día de entrenamiento que no es hoy
        WeeklyPlan weeklyPlan = new WeeklyPlan(List.of(
            new PlanDay(todayIndex, "Hoy", "Fuerza", false, List.of(
                new PlanExercise(0, "Sentadilla", 3, "10", 60.0, 90, null))),
            new PlanDay(otherDayIndex, "Otro día", "Cardio", false, List.of(
                new PlanExercise(0, "Burpees", 4, "12", null, 45, null)))));
        LocalDate weekStart = LocalDate.now(ZoneId.of("Europe/Madrid")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WorkoutPlan workoutPlan = new WorkoutPlan(
            tenant.getId(), user.getId(), weekStart, "Fuerza", "Intermedio", 2,
            workoutService.serialize(weeklyPlan), PlanSource.MANUAL);
        return workoutPlans.save(workoutPlan);
    }

    @Test
    void memberSeesTheFullWeekWithTodayHighlightedAndRestDaysForTheRest() throws Exception {
        User user = createMemberWithMembership("semana.completa@test.local");
        createTwoDayPlanForUser(user);
        String token = loginAndGetToken("semana.completa@test.local");
        int otherDayIndex = (todayIndex + 2) % 7;

        mvc.perform(get("/api/v1/me/workout/week").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(true))
            .andExpect(jsonPath("$.todayIndex").value(todayIndex))
            .andExpect(jsonPath("$.days", org.hamcrest.Matchers.hasSize(7)))
            .andExpect(jsonPath("$.days[" + todayIndex + "].restDay").value(false))
            .andExpect(jsonPath("$.days[" + todayIndex + "].exercises[0].name").value("Sentadilla"))
            .andExpect(jsonPath("$.days[" + otherDayIndex + "].exercises[0].name").value("Burpees"))
            .andExpect(jsonPath("$.days[" + ((todayIndex + 1) % 7) + "].restDay").value(true));
    }

    @Test
    void weekTracksLoggedSetsSeparatelyPerDayEvenWithTheSameExerciseIndex() throws Exception {
        User user = createMemberWithMembership("series.por.dia@test.local");
        createTwoDayPlanForUser(user);
        String token = loginAndGetToken("series.por.dia@test.local");
        int otherDayIndex = (todayIndex + 2) % 7;

        // Marca la serie de HOY (exerciseIndex 0) hecha: el otro día usa el mismo índice 0
        // para un ejercicio distinto y no debería contaminarse.
        String body = objectMapper.writeValueAsString(new LogSetRequest(todayIndex, 0, 0, true, null, null));
        mvc.perform(post("/api/v1/me/workout/logs").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/workout/week").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days[" + todayIndex + "].exercises[0].sets[0].done").value(true))
            .andExpect(jsonPath("$.days[" + otherDayIndex + "].exercises[0].sets[0].done").value(false));
    }

    @Test
    void restDayPointsToTheNextTrainingDayInsteadOfSayingNothing() throws Exception {
        User user = createMemberWithMembership("descanso.con.proximo@test.local");
        int trainingDayIndex = (todayIndex + 1) % 7; // el único día de entrenamiento: mañana
        WeeklyPlan weeklyPlan = new WeeklyPlan(List.of(
            new PlanDay(trainingDayIndex, "Día de entreno", "Pierna", false, List.of(
                new PlanExercise(0, "Sentadilla", 3, "10", 60.0, 90, null)))));
        LocalDate weekStart = LocalDate.now(ZoneId.of("Europe/Madrid")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        workoutPlans.save(new WorkoutPlan(
            tenant.getId(), user.getId(), weekStart, "Fuerza", "Intermedio", 1,
            workoutService.serialize(weeklyPlan), PlanSource.MANUAL));
        String token = loginAndGetToken("descanso.con.proximo@test.local");

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restDay").value(true))
            .andExpect(jsonPath("$.nextLabel").value("Día de entreno"))
            .andExpect(jsonPath("$.nextFocus").value("Pierna"));
    }

    @Test
    void memberWithoutPlanGetsHasPlanFalseForTheWeekToo() throws Exception {
        createMemberWithMembership("sin.rutina.semana@test.local");
        String token = loginAndGetToken("sin.rutina.semana@test.local");

        mvc.perform(get("/api/v1/me/workout/week").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPlan").value(false))
            .andExpect(jsonPath("$.days", org.hamcrest.Matchers.hasSize(0)));
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

    @Test
    void generateSavesAndReturnsTheAiPlanWhenTariffIncludesAiTrainer() throws Exception {
        createMemberWithMembership("genera.rutina@test.local");
        String token = loginAndGetToken("genera.rutina@test.local");
        Mockito.when(trainerClient.generatePlan(Mockito.any()))
            .thenReturn(fakeAiPlan("Dos días de cuerpo completo", 2));

        String body = objectMapper.writeValueAsString(
            new GenerateWorkoutRequest("hypertrophy", "beginner", 2, List.of("gimnasio completo"), List.of()));

        mvc.perform(post("/api/v1/me/workout/generate").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days.length()").value(2))
            .andExpect(jsonPath("$.days[0].exercises[0].name").value("Sentadilla"));

        mvc.perform(get("/api/v1/me/workout/today").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void generateIsForbiddenWhenTariffHasNoAiTrainer_andNeverCallsTheAiService() throws Exception {
        createMemberWithMembership("sin.ia@test.local", planWithoutAiTrainer);
        String token = loginAndGetToken("sin.ia@test.local");

        String body = objectMapper.writeValueAsString(
            new GenerateWorkoutRequest("hypertrophy", "beginner", 2, List.of(), List.of()));

        mvc.perform(post("/api/v1/me/workout/generate").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(trainerClient);
    }

    @Test
    void generateReturns503WhenAiServiceIsUnreachable() throws Exception {
        createMemberWithMembership("ia.caida@test.local");
        String token = loginAndGetToken("ia.caida@test.local");
        Mockito.when(trainerClient.generatePlan(Mockito.any())).thenThrow(new TrainerUnavailableException());

        String body = objectMapper.writeValueAsString(
            new GenerateWorkoutRequest("strength", "advanced", 3, List.of(), List.of()));

        mvc.perform(post("/api/v1/me/workout/generate").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void generatingAgainReplacesThePreviousPlanAndItsLogsForTheSameWeek() throws Exception {
        User user = createMemberWithMembership("regenera@test.local");
        String token = loginAndGetToken("regenera@test.local");
        Mockito.when(trainerClient.generatePlan(Mockito.any()))
            .thenReturn(fakeAiPlan("Primera versión", 1))
            .thenReturn(fakeAiPlan("Segunda versión", 1));

        String body = objectMapper.writeValueAsString(
            new GenerateWorkoutRequest("general", "beginner", 1, List.of(), List.of()));

        mvc.perform(post("/api/v1/me/workout/generate").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        LocalDate weekStart = LocalDate.now(ZoneId.of("Europe/Madrid")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WorkoutPlan firstPlan = workoutPlans.findByTenantIdAndUserIdAndWeekStart(tenant.getId(), user.getId(), weekStart).orElseThrow();
        workoutLogs.save(new WorkoutLog(tenant.getId(), user.getId(), firstPlan.getId(), 0, 0, 0, true, null, null));

        mvc.perform(post("/api/v1/me/workout/generate").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days[0].focus").value("Enfoque día 1"));

        assertTrue(workoutLogs.findByWorkoutPlanId(firstPlan.getId()).isEmpty(), "las series de la rutina anterior deben desaparecer con ella");
        WorkoutPlan secondPlan = workoutPlans.findByTenantIdAndUserIdAndWeekStart(tenant.getId(), user.getId(), weekStart).orElseThrow();
        assertTrue(!secondPlan.getId().equals(firstPlan.getId()), "debe ser una fila nueva, no la misma reutilizada");
    }
}
