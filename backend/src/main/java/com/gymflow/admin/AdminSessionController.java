package com.gymflow.admin;

import com.gymflow.admin.dto.CreateSessionRequest;
import com.gymflow.admin.dto.UpdateSessionRequest;
import com.gymflow.booking.ScheduleService;
import com.gymflow.booking.dto.SessionSummary;
import com.gymflow.common.TenantContext;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de sesiones de clase (LOGIC.md §6). OWNER y STAFF por igual. */
@RestController
@RequestMapping("/api/v1/admin/sessions")
public class AdminSessionController {

    private static final long DEFAULT_WINDOW_DAYS = 30;

    private final AdminSessionService adminSessionService;
    private final ScheduleService scheduleService;

    public AdminSessionController(AdminSessionService adminSessionService, ScheduleService scheduleService) {
        this.adminSessionService = adminSessionService;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<SessionSummary> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String discipline) {
        Instant fromInstant = parseOrDefault(from, Instant.now().minusSeconds(24 * 3600));
        Instant toInstant = parseOrDefault(to, fromInstant.plusSeconds(DEFAULT_WINDOW_DAYS * 24 * 3600));
        return scheduleService.listSchedule(TenantContext.require(), fromInstant, toInstant, discipline, null, null);
    }

    @PostMapping
    public ResponseEntity<List<SessionSummary>> create(@Valid @RequestBody CreateSessionRequest request) {
        List<SessionSummary> created = adminSessionService.create(TenantContext.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public SessionSummary update(@PathVariable UUID id, @RequestBody UpdateSessionRequest request) {
        return adminSessionService.update(TenantContext.require(), id, request);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Integer> cancel(@PathVariable UUID id) {
        int affected = adminSessionService.cancel(TenantContext.require(), id);
        return Map.of("cancelledBookings", affected);
    }

    private Instant parseOrDefault(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida: " + value);
        }
    }
}
