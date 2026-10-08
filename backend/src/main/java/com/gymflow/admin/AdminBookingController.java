package com.gymflow.admin;

import com.gymflow.admin.dto.AdminBookingSummary;
import com.gymflow.common.TenantContext;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Listado de reservas (LOGIC.md §6) y cancelación sin límite de 2h por staff/owner (§5.3). */
@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private static final long DEFAULT_WINDOW_DAYS = 30;

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public List<AdminBookingSummary> list(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        Instant fromInstant = parseOrDefault(from, Instant.now().minusSeconds(24 * 3600));
        Instant toInstant = parseOrDefault(to, fromInstant.plusSeconds(DEFAULT_WINDOW_DAYS * 24 * 3600));
        return adminBookingService.list(TenantContext.require(), fromInstant, toInstant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        adminBookingService.cancel(TenantContext.require(), id);
        return ResponseEntity.noContent().build();
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
