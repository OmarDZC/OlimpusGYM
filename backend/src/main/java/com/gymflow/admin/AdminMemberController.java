package com.gymflow.admin;

import com.gymflow.admin.dto.ChangePlanRequest;
import com.gymflow.admin.dto.MemberSummary;
import com.gymflow.common.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Listado de socios (LOGIC.md §6) y cambio de tarifa (solo OWNER). */
@RestController
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {

    private final AdminMemberService memberService;

    public AdminMemberController(AdminMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<MemberSummary> list(Authentication authentication) {
        boolean isOwner = AdminAuth.isOwner(authentication);
        return memberService.list(TenantContext.require(), isOwner);
    }

    @GetMapping("/{id}")
    public MemberSummary detail(@PathVariable UUID id, Authentication authentication) {
        boolean isOwner = AdminAuth.isOwner(authentication);
        return memberService.detail(TenantContext.require(), id, isOwner);
    }

    // Solo OWNER: lo exige SecurityConfig (PATCH .../admin/members/{id}/plan), no este método.
    @PatchMapping("/{id}/plan")
    public MemberSummary changePlan(
            @PathVariable UUID id, @Valid @RequestBody ChangePlanRequest request, Authentication authentication) {
        UUID ownerUserId = (UUID) authentication.getPrincipal();
        return memberService.changePlan(TenantContext.require(), ownerUserId, id, request.planCode(), true);
    }
}
