package com.gymflow.plan.dto;

import com.gymflow.plan.PlanCode;
import jakarta.validation.constraints.NotNull;

public record PlanChangeRequest(@NotNull(message = "Elige una tarifa") PlanCode planCode) {
}
