package com.gymflow.workout.dto;

public record StreakDay(int dayIndex, String label, boolean trained, boolean restDay, boolean isToday) {
}
