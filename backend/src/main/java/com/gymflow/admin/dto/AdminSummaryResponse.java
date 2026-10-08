package com.gymflow.admin.dto;

import java.util.List;

/**
 * {@code monthlyRevenueCents} solo para OWNER (LOGIC.md §2: "ver ingresos" es cosa del dueño);
 * para STAFF llega a null.
 */
public record AdminSummaryResponse(
    int activeMembers,
    int todaySessions,
    int todayBookedSeats,
    int todayCapacity,
    Integer monthlyRevenueCents,
    List<MemberToContact> membersToContact) {
}
