package com.gymflow.admin;

import com.gymflow.admin.dto.ContactMessageSummary;
import com.gymflow.common.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mensajes del formulario de contacto (Trozo 7b, LOGIC.md §6): OWNER y STAFF, como el resto del panel. */
@RestController
@RequestMapping("/api/v1/admin/messages")
public class AdminMessageController {

    private final AdminMessageService adminMessageService;

    public AdminMessageController(AdminMessageService adminMessageService) {
        this.adminMessageService = adminMessageService;
    }

    @GetMapping
    public List<ContactMessageSummary> list() {
        return adminMessageService.list(TenantContext.require());
    }

    @PatchMapping("/{id}/read")
    public ContactMessageSummary markRead(@PathVariable UUID id) {
        return adminMessageService.markRead(TenantContext.require(), id);
    }
}
