package com.gymflow.workout;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    Optional<WorkoutPlan> findByTenantIdAndUserIdAndWeekStart(UUID tenantId, UUID userId, LocalDate weekStart);
}
