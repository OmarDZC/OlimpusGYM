package com.gymflow.admin.dto;

import java.util.UUID;

/**
 * {@code priceCents} solo viene relleno para OWNER (LOGIC.md §2: STAFF ve socios "sin datos
 * de pago"); para STAFF llega a null y el frontend no lo muestra.
 */
public record MemberSummary(
    UUID userId,
    String fullName,
    String email,
    String planCode,
    String planName,
    String membershipStatus,
    Integer priceCents) {
}
