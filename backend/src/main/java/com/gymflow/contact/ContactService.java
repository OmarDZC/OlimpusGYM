package com.gymflow.contact;

import com.gymflow.contact.dto.ContactRequest;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Formulario público de contacto (Trozo 7a, LOGIC.md §6). */
@Service
public class ContactService {

    private final ContactMessageRepository messages;
    private final TenantRepository tenants;
    private final ContactNotifier notifier;
    private final ContactRateLimiter rateLimiter;
    private final Clock clock;

    public ContactService(
            ContactMessageRepository messages,
            TenantRepository tenants,
            ContactNotifier notifier,
            ContactRateLimiter rateLimiter,
            Clock clock) {
        this.messages = messages;
        this.tenants = tenants;
        this.notifier = notifier;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public void submit(ContactRequest request, String clientIp) {
        if (request.website() != null && !request.website().isBlank()) {
            // Campo trampa relleno: lo manda un bot, no una persona. No se distingue del resto
            // de errores de validación para no darle pistas de por qué falló.
            throw new IllegalArgumentException("No se pudo enviar el mensaje.");
        }
        if (!rateLimiter.allow(clientIp)) {
            throw new TooManyContactAttemptsException();
        }

        UUID tenantId = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseThrow(() -> new IllegalStateException("No existe el tenant por defecto"))
                .getId();

        ContactMessage message = new ContactMessage(
                tenantId, request.name().trim(), request.email().trim(), request.message().trim(), clock.instant());
        messages.save(message);
        notifier.received(message);
    }
}
