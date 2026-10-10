package com.gymflow.booking;

import com.gymflow.booking.dto.PublicLiveStatusDto;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Franja "En directo" de la portada (Trozo 7b, LOGIC.md §9): sin sesión, con caché corta (la
 * propia franja ya se refresca sola cada 30-60 s desde el frontend; esta cabecera solo evita
 * peticiones repetidas en ese hueco, p. ej. varias pestañas abiertas a la vez).
 */
@RestController
@RequestMapping("/api/v1/public/live")
public class PublicLiveController {

    private static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");

    private final ScheduleService scheduleService;
    private final TenantRepository tenants;
    private final Clock clock;

    public PublicLiveController(ScheduleService scheduleService, TenantRepository tenants, Clock clock) {
        this.scheduleService = scheduleService;
        this.tenants = tenants;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<PublicLiveStatusDto> live() {
        UUID tenantId = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseThrow(() -> new IllegalStateException("No existe el tenant por defecto"))
                .getId();

        Instant now = clock.instant();
        // "Hoy" en Europe/Madrid, no en UTC: a medianoche en Madrid, una clase de madrugada en
        // UTC todavía podría ser "mañana" o "hoy" según el cambio de hora, así que se calcula
        // siempre con la zona real, nunca restando horas a mano.
        Instant endOfTodayMadrid = now.atZone(MADRID_ZONE).toLocalDate().plusDays(1).atStartOfDay(MADRID_ZONE).toInstant();

        PublicLiveStatusDto status = scheduleService.liveStatus(tenantId, now, endOfTodayMadrid);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(10)).cachePublic())
                .body(status);
    }
}
