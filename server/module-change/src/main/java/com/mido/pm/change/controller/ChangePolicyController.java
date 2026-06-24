package com.mido.pm.change.controller;

import com.mido.pm.change.dto.ChangePolicyUpsertDTO;
import com.mido.pm.change.dto.ChangePolicyVO;
import com.mido.pm.change.service.ChangePolicyService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 变更策略管理（租户自配各变更类型的必审/免审 + 审批流绑定）。 */
@RestController
@RequestMapping("/api/v1/change-policies")
public class ChangePolicyController {

    private final ChangePolicyService changePolicyService;

    public ChangePolicyController(ChangePolicyService changePolicyService) {
        this.changePolicyService = changePolicyService;
    }

    /** 全部变更策略（前端按 changeType 字典合并展示，无策略者默认免审）。 */
    @GetMapping
    public R<List<ChangePolicyVO>> list() {
        return R.ok(changePolicyService.list());
    }

    /** 按 changeType 幂等保存（必审需绑流）。 */
    @PutMapping
    public R<Void> save(@Valid @RequestBody ChangePolicyUpsertDTO dto) {
        changePolicyService.save(dto);
        return R.ok();
    }
}
