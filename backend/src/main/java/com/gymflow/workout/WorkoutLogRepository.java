package com.gymflow.workout;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

    List<WorkoutLog> findByWorkoutPlanId(UUID workoutPlanId);

    List<WorkoutLog> findByWorkoutPlanIdAndDayIndex(UUID workoutPlanId, int dayIndex);

    Optional<WorkoutLog> findByWorkoutPlanIdAndDayIndexAndExerciseIndexAndSetIndex(
            UUID workoutPlanId, int dayIndex, int exerciseIndex, int setIndex);
}
