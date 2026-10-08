package com.gymflow.workout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, length = 60)
    private String goal;

    @Column(nullable = false, length = 30)
    private String level;

    @Column(name = "days_per_week", nullable = false)
    private int daysPerWeek;

    /** JSON serializado con Jackson (ver {@code com.gymflow.workout.dto.WeeklyPlan}). */
    @Column(name = "plan_json", nullable = false, columnDefinition = "text")
    private String planJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlanSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WorkoutPlan() {
    }

    public WorkoutPlan(
            UUID tenantId,
            UUID userId,
            LocalDate weekStart,
            String goal,
            String level,
            int daysPerWeek,
            String planJson,
            PlanSource source) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.weekStart = weekStart;
        this.goal = goal;
        this.level = level;
        this.daysPerWeek = daysPerWeek;
        this.planJson = planJson;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public String getGoal() {
        return goal;
    }

    public String getLevel() {
        return level;
    }

    public int getDaysPerWeek() {
        return daysPerWeek;
    }

    public String getPlanJson() {
        return planJson;
    }

    public PlanSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
