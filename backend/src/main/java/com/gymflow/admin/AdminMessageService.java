package com.gymflow.admin;

import com.gymflow.admin.dto.ContactMessageSummary;
import com.gymflow.contact.ContactMessage;
import com.gymflow.contact.ContactMessageNotFoundException;
import com.gymflow.contact.ContactMessageRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mensajes de contacto en el panel de gestión (Trozo 7b, LOGIC.md §6): OWNER y STAFF por igual. */
@Service
public class AdminMessageService {

    private final ContactMessageRepository messages;
    private final Clock clock;

    public AdminMessageService(ContactMessageRepository messages, Clock clock) {
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ContactMessageSummary> list(UUID tenantId) {
        return messages.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toSummary).toList();
    }

    @Transactional
    public ContactMessageSummary markRead(UUID tenantId, UUID messageId) {
        ContactMessage message = messages.findById(messageId)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(ContactMessageNotFoundException::new);
        if (message.getReadAt() == null) {
            message.markRead(clock.instant());
            messages.save(message);
        }
        return toSummary(message);
    }

    private ContactMessageSummary toSummary(ContactMessage message) {
        return new ContactMessageSummary(
                message.getId(),
                message.getName(),
                message.getEmail(),
                message.getMessage(),
                message.getCreatedAt(),
                message.getReadAt() != null);
    }
}
