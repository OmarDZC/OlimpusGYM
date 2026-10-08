package com.gymflow.workout.dto;

import java.util.List;

public record StreakResponse(boolean hasPlan, int targetDays, int completedDays, List<StreakDay> days) {

    public static StreakResponse noPlan() {
        return new StreakResponse(false, 0, 0, List.of());
    }
}
