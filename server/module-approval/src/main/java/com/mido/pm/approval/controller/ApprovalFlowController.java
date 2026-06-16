package com.mido.pm.approval.controller;

import com.mido.pm.approval.dto.FlowCreateDTO;
import com.mido.pm.approval.dto.FlowVO;
import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 审批流定义管理。 */
@RestController
@RequestMapping("/api/v1/approval-flows")
public class ApprovalFlowController {

    private final ApprovalFlowService flowService;

    public ApprovalFlowController(ApprovalFlowService flowService) {
        this.flowService = flowService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody FlowCreateDTO dto) {
        return R.ok(flowService.create(dto));
    }

    @GetMapping
    public R<List<FlowVO>> list(@RequestParam(required = false) String bizType) {
        return R.ok(flowService.list(bizType));
    }

    @GetMapping("/{id}")
    public R<FlowVO> get(@PathVariable Long id) {
        return R.ok(flowService.get(id));
    }
}
