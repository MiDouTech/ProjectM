package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.ApiKeyCreateDTO;
import com.mido.pm.org.dto.ApiKeyCreatedVO;
import com.mido.pm.org.dto.ApiKeyVO;
import com.mido.pm.org.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 开放平台 API Key 管理（租户侧）。创建返回的明文仅展示一次。 */
@RestController
@RequestMapping("/api/v1/apikeys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public R<List<ApiKeyVO>> list() {
        return R.ok(apiKeyService.list());
    }

    @PreAuthorize("hasAuthority('org:apikey:manage')")
    @PostMapping
    public R<ApiKeyCreatedVO> create(@Valid @RequestBody ApiKeyCreateDTO dto) {
        return R.ok(apiKeyService.create(dto));
    }

    @PreAuthorize("hasAuthority('org:apikey:manage')")
    @PutMapping("/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id) {
        apiKeyService.revoke(id);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('org:apikey:manage')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return R.ok();
    }
}
