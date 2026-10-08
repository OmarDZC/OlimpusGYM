package com.gymflow.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.workout.dto.ExerciseProgress;
import com.gymflow.workout.dto.LogSetRequest;
import com.gymflow.workout.dto.PlanDay;
import com.gymflow.workout.dto.PlanExercise;
import com.gymflow.workout.dto.SetProgress;
import com.gymflow.workout.dto.StreakDay;
import com.gymflow.workout.dto.StreakResponse;
import com.gymflow.workout.dto.TodayWorkoutResponse;
import com.gymflow.workout.dto.WeeklyPlan;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rutina de la semana del socio (LOGIC.md §6): "hoy", constancia semanal y marcar series.
 * La generación real con IA llega en el Trozo 5; aquí solo se lee lo que ya exista
 * (por ahora, de seed/manual).
 */
@Service
public class WorkoutService {

    private static final ZoneId MEMBER_ZONE = ZoneId.of("Europe/Madrid");
    private static final String[] DAY_LABELS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};

    private final WorkoutPlanRepository plans;
    private final WorkoutLogRepository logs;
    private final ObjectMapper objectMapper;

    public WorkoutService(WorkoutPlanRepository plans, WorkoutLogRepository logs, ObjectMapper objectMapper) {
        this.plans = plans;
        this.logs = logs;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TodayWorkoutResponse getToday(UUID tenantId, UUID userId) {
        LocalDate today = LocalDate.now(MEMBER_ZONE);
        int todayIndex = dayIndexOf(today);
        WorkoutPlan plan = currentPlan(tenantId, userId, today).orElse(null);
        if (plan == null) {
            return TodayWorkoutResponse.noPlan(todayIndex);
        }

        WeeklyPlan weeklyPlan = parse(plan.getPlanJson());
        PlanDay day = dayByIndex(weeklyPlan, todayIndex);
        if (day == null || day.restDay()) {
            return TodayWorkoutResponse.rest(todayIndex, DAY_LABELS[todayIndex]);
        }

        Map<String, WorkoutLog> loggedBySetKey = logs.findByWorkoutPlanIdAndDayIndex(plan.getId(), todayIndex).stream()
                .collect(Collectors.toMap(l -> l.getExerciseIndex() + ":" + l.getSetIndex(), l -> l));

        List<ExerciseProgress> exercises =
                day.exercises().stream().map(exercise -> toProgress(exercise, loggedBySetKey)).toList();

        return new TodayWorkoutResponse(true, false, todayIndex, day.label(), day.focus(), exercises);
    }

    @Transactional(readOnly = true)
    public StreakResponse getStreak(UUID tenantId, UUID userId) {
        LocalDate today = LocalDate.now(MEMBER_ZONE);
        WorkoutPlan plan = currentPlan(tenantId, userId, today).orElse(null);
        if (plan == null) {
            return StreakResponse.noPlan();
        }

        WeeklyPlan weeklyPlan = parse(plan.getPlanJson());
        int todayIndex = dayIndexOf(today);
        List<WorkoutLog> weekLogs = logs.findByWorkoutPlanId(plan.getId());

        List<StreakDay> days = IntStream.range(0, 7)
                .mapToObj(i -> {
                    PlanDay day = dayByIndex(weeklyPlan, i);
                    boolean restDay = day == null || day.restDay();
                    boolean trained =
                            !restDay && weekLogs.stream().anyMatch(l -> l.getDayIndex() == i && l.isDone());
                    return new StreakDay(i, DAY_LABELS[i], trained, restDay, i == todayIndex);
                })
                .toList();

        long targetDays = weeklyPlan.days().stream().filter(d -> !d.restDay()).count();
        long completedDays = days.stream().filter(StreakDay::trained).count();

        return new StreakResponse(true, (int) targetDays, (int) completedDays, days);
    }

    @Transactional
    public void logSet(UUID tenantId, UUID userId, LogSetRequest request) {
        LocalDate today = LocalDate.now(MEMBER_ZONE);
        WorkoutPlan plan = currentPlan(tenantId, userId, today)
                .orElseThrow(() -> new IllegalArgumentException("No tienes una rutina activa esta semana."));

        WeeklyPlan weeklyPlan = parse(plan.getPlanJson());
        PlanDay day = dayByIndex(weeklyPlan, request.dayIndex());
        if (day == null || day.restDay()) {
            throw new IllegalArgumentException("Ese día no tiene rutina.");
        }
        PlanExercise exercise = day.exercises().stream()
                .filter(e -> e.exerciseIndex() == request.exerciseIndex())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ese ejercicio no existe en tu rutina."));
        if (request.setIndex() >= exercise.sets()) {
            throw new IllegalArgumentException("Esa serie no existe en tu rutina.");
        }

        BigDecimal weight = request.weightKg() != null ? BigDecimal.valueOf(request.weightKg()) : null;

        WorkoutLog log = logs.findByWorkoutPlanIdAndDayIndexAndExerciseIndexAndSetIndex(
                        plan.getId(), request.dayIndex(), request.exerciseIndex(), request.setIndex())
                .orElse(null);
        if (log == null) {
            log = new WorkoutLog(
                    tenantId, userId, plan.getId(), request.dayIndex(), request.exerciseIndex(), request.setIndex(),
                    request.done(), weight, request.reps());
        } else {
            log.update(request.done(), weight, request.reps());
        }
        logs.save(log);
    }

    /** Lo usa también el seed de datos de ejemplo para guardar la rutina de Carlos. */
    public String serialize(WeeklyPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la rutina", e);
        }
    }

    private Optional<WorkoutPlan> currentPlan(UUID tenantId, UUID userId, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return plans.findByTenantIdAndUserIdAndWeekStart(tenantId, userId, weekStart);
    }

    private ExerciseProgress toProgress(PlanExercise exercise, Map<String, WorkoutLog> loggedBySetKey) {
        List<SetProgress> sets = IntStream.range(0, exercise.sets())
                .mapToObj(setIndex -> {
                    WorkoutLog log = loggedBySetKey.get(exercise.exerciseIndex() + ":" + setIndex);
                    return new SetProgress(setIndex, log != null && log.isDone());
                })
                .toList();
        return new ExerciseProgress(
                exercise.exerciseIndex(), exercise.name(), exercise.reps(), exercise.weightKg(),
                exercise.restSeconds(), exercise.note(), sets);
    }

    private PlanDay dayByIndex(WeeklyPlan plan, int dayIndex) {
        return plan.days().stream().filter(d -> d.dayIndex() == dayIndex).findFirst().orElse(null);
    }

    private int dayIndexOf(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1;
    }

    private WeeklyPlan parse(String json) {
        try {
            return objectMapper.readValue(json, WeeklyPlan.class);
        } catch (Exception e) {
            throw new IllegalStateException("La rutina guardada no se puede leer", e);
        }
    }
}
