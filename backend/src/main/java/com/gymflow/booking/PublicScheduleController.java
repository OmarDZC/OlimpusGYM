package com.gymflow.booking;

import com.gymflow.booking.dto.SessionSummary;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Horario público (LOGIC.md §6): no requiere sesión, solo muestra sesiones con sus plazas libres. */
@RestController
@RequestMapping("/api/v1/public/schedule")
public class PublicScheduleController {

    private static final long DEFAULT_WINDOW_DAYS = 14;

    private final ScheduleService scheduleService;
    private final TenantRepository tenants;

    public PublicScheduleController(ScheduleService scheduleService, TenantRepository tenants) {
        this.scheduleService = scheduleService;
        this.tenants = tenants;
    }

    @GetMapping
    public List<SessionSummary> schedule(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String discipline,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) String room) {
        UUID tenantId = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseThrow(() -> new IllegalStateException("No existe el tenant por defecto"))
                .getId();

        Instant fromInstant = parseOrDefault(from, Instant.now());
        Instant toInstant = parseOrDefault(to, fromInstant.plusSeconds(DEFAULT_WINDOW_DAYS * 24 * 3600));

        return scheduleService.listSchedule(tenantId, fromInstant, toInstant, discipline, coachId, room);
    }

    private Instant parseOrDefault(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida: " + value + " (usa formato ISO-8601, p. ej. 2026-10-10T00:00:00Z)");
        }
    }
}
