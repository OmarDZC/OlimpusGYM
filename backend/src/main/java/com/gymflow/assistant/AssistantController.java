package com.gymflow.assistant;

import com.gymflow.assistant.dto.AssistantChatRequest;
import com.gymflow.assistant.dto.AssistantChatResponse;
import com.gymflow.common.TenantContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(
            Authentication authentication,
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody AssistantChatRequest request) {
        String token = authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "");
        return assistantService.chat(TenantContext.require(), userId(authentication), token, request);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
