package com.gymflow.workout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_logs")
public class WorkoutLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workout_plan_id", nullable = false)
    private UUID workoutPlanId;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "exercise_index", nullable = false)
    private int exerciseIndex;

    @Column(name = "set_index", nullable = false)
    private int setIndex;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "weight_kg")
    private java.math.BigDecimal weightKg;

    private Integer reps;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();

    protected WorkoutLog() {
    }

    public WorkoutLog(
            UUID tenantId,
            UUID userId,
            UUID workoutPlanId,
            int dayIndex,
            int exerciseIndex,
            int setIndex,
            boolean done,
            java.math.BigDecimal weightKg,
            Integer reps) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.workoutPlanId = workoutPlanId;
        this.dayIndex = dayIndex;
        this.exerciseIndex = exerciseIndex;
        this.setIndex = setIndex;
        this.done = done;
        this.weightKg = weightKg;
        this.reps = reps;
    }

    public void update(boolean done, java.math.BigDecimal weightKg, Integer reps) {
        this.done = done;
        this.weightKg = weightKg;
        this.reps = reps;
        this.loggedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkoutPlanId() {
        return workoutPlanId;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public int getExerciseIndex() {
        return exerciseIndex;
    }

    public int getSetIndex() {
        return setIndex;
    }

    public boolean isDone() {
        return done;
    }

    public java.math.BigDecimal getWeightKg() {
        return weightKg;
    }

    public Integer getReps() {
        return reps;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }
}
