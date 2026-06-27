package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.SubscriptionSaveDTO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 租户订阅：绑定/续期套餐（同步租户到期与状态）。 */
@Tag(name = "平台-租户订阅", description = "绑定/续期套餐")
@RestController
@RequestMapping("/api/v1/platform/tenants/{tenantId}/subscription")
public class TenantSubscriptionController {

    private final PlatformSubscriptionService subscriptionService;

    public TenantSubscriptionController(PlatformSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.SUBSCRIPTION_MANAGE + "')")
    @Operation(summary = "绑定/续期订阅", description = "绑定套餐并同步租户到期与状态")
    @PostMapping
    public R<Void> bind(@PathVariable Long tenantId, @Valid @RequestBody SubscriptionSaveDTO dto) {
        subscriptionService.bind(tenantId, dto);
        return R.ok();
    }
}
