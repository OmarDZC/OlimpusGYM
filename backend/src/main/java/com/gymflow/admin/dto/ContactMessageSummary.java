package com.gymflow.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageSummary(
        UUID id, String name, String email, String message, Instant createdAt, boolean read) {
}
