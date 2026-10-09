package com.gymflow.plan.dto;

/** Una tarifa tal y como se enseña en la web pública (Trozo 7a, LOGIC.md §3), sin precio del socio. */
public record PublicPlanDto(
        String planCode,
        String planName,
        int priceCents,
        Integer classesPerMonth,
        boolean aiTrainer,
        boolean aiAssistant) {
}
