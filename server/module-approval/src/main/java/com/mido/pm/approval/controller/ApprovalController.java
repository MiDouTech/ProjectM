package com.mido.pm.approval.controller;

import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.InstanceVO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 通用审批：提交、审批动作、实例查询。 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/submit")
    public R<Long> submit(@Valid @RequestBody SubmitDTO dto) {
        return R.ok(approvalService.submit(dto));
    }

    /** 审批动作（approve/reject）。 */
    @PostMapping("/instances/{id}/actions")
    public R<Void> act(@PathVariable Long id, @Valid @RequestBody ActDTO dto) {
        approvalService.act(id, dto);
        return R.ok();
    }

    @GetMapping("/instances/{id}")
    public R<InstanceVO> getInstance(@PathVariable Long id) {
        return R.ok(approvalService.getInstance(id));
    }
}
