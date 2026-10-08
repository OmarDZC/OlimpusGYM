package com.gymflow.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberToContact(UUID userId, String fullName, String email, Instant lastBookingAt) {
}
