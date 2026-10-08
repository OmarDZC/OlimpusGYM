package com.gymflow.admin;

import com.gymflow.admin.dto.AdminSummaryResponse;
import com.gymflow.common.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/summary")
public class AdminSummaryController {

    private final AdminSummaryService summaryService;

    public AdminSummaryController(AdminSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public AdminSummaryResponse summary(Authentication authentication) {
        boolean isOwner = AdminAuth.isOwner(authentication);
        return summaryService.summary(TenantContext.require(), isOwner);
    }
}
