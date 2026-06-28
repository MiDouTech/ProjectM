package com.mido.pm.approval.controller;

import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.ApprovalBizTypeVO;
import com.mido.pm.approval.dto.InitiatedApprovalVO;
import com.mido.pm.approval.dto.InstanceVO;
import com.mido.pm.approval.dto.PendingApprovalVO;
import com.mido.pm.approval.dto.RelatedApprovalVO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.dto.TransferDTO;
import com.mido.pm.approval.dto.WithdrawDTO;
import com.mido.pm.approval.outcome.ApprovalBizTypeRegistry;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;

import java.util.List;
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
    private final ApprovalBizTypeRegistry bizTypeRegistry;

    public ApprovalController(ApprovalService approvalService, ApprovalBizTypeRegistry bizTypeRegistry) {
        this.approvalService = approvalService;
        this.bizTypeRegistry = bizTypeRegistry;
    }

    /** 审批 bizType 字典（单一信息源）：供前端筛选下拉与审批流设计器消费。 */
    @GetMapping("/biz-types")
    public R<List<ApprovalBizTypeVO>> bizTypes() {
        return R.ok(bizTypeRegistry.list());
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

    /** 发起人撤回（仅 pending + 仅申请人本人）；body.reason 选填。 */
    @PostMapping("/instances/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id, @RequestBody(required = false) WithdrawDTO dto) {
        approvalService.withdraw(id, dto);
        return R.ok();
    }

    /** 转交：当前审批人把待办交给他人；body.toUserId 必填、comment 选填。 */
    @PostMapping("/instances/{id}/transfer")
    public R<Void> transfer(@PathVariable Long id, @Valid @RequestBody TransferDTO dto) {
        approvalService.transfer(id, dto);
        return R.ok();
    }

    /** 待我审批（工作台卡）：当前用户未处理且实例仍 pending 的待办列表。 */
    @GetMapping("/mine")
    public R<List<PendingApprovalVO>> mine() {
        return R.ok(approvalService.myPending());
    }

    /** 我发起的（审批中心）：当前用户提交的审批实例，跨 bizType、任意状态。 */
    @GetMapping("/mine-initiated")
    public R<List<InitiatedApprovalVO>> mineInitiated() {
        return R.ok(approvalService.myInitiated());
    }

    /** 与我相关的全部审批（审批中心「全部」列表）：我发起 ∪ 待我处理 ∪ 我已处理，跨 bizType、任意状态。 */
    @GetMapping("/mine-all")
    public R<List<RelatedApprovalVO>> mineAll() {
        return R.ok(approvalService.myRelated());
    }
}
