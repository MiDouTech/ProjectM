package com.mido.pm.goal.controller;

import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.common.api.R;
import com.mido.pm.goal.dto.GoalChangeRequestDTO;
import com.mido.pm.goal.service.GoalChangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 目标变更：发起变更（受控，复用审批引擎）+ 查看本目标变更历史。台账总览见 /api/v1/changes。 */
@RestController
@RequestMapping("/api/v1/goals/{goalId}/changes")
public class GoalChangeController {

    private final GoalChangeService goalChangeService;

    public GoalChangeController(GoalChangeService goalChangeService) {
        this.goalChangeService = goalChangeService;
    }

    /** 发起目标变更，返回变更单 id。 */
    @PostMapping
    public R<Long> submit(@PathVariable Long goalId, @Valid @RequestBody GoalChangeRequestDTO dto) {
        return R.ok(goalChangeService.submit(goalId, dto));
    }

    /** 本目标的变更历史。 */
    @GetMapping
    public R<List<ChangeRequestVO>> list(@PathVariable Long goalId) {
        return R.ok(goalChangeService.list(goalId));
    }
}
