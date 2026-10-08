package com.gymflow.admin.dto;

import com.gymflow.plan.PlanCode;
import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(@NotNull PlanCode planCode) {
}
