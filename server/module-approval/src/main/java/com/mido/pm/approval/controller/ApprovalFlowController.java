package com.mido.pm.approval.controller;

import com.mido.pm.approval.dto.FlowCreateDTO;
import com.mido.pm.approval.dto.FlowVO;
import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 审批流定义管理（兼作 ApprovalFlowDesigner 可视化设计器的后端接口）。 */
@RestController
@RequestMapping("/api/v1/approval-flows")
public class ApprovalFlowController {

    private final ApprovalFlowService flowService;

    public ApprovalFlowController(ApprovalFlowService flowService) {
        this.flowService = flowService;
    }

    /**
     * 设计器元数据占位：返回可用节点模式/可插拔 guard/条件字段，供前端 ApprovalFlowDesigner 渲染。
     * 设计器 UI 放 P1；设计器的读/存流程定义复用本控制器 GET /{id} 与 POST。
     */
    @GetMapping("/designer-meta")
    public R<Map<String, Object>> designerMeta() {
        return R.ok(Map.of(
                "modes", List.of("or", "and"),
                "guards", List.of("JOB_LEVEL"),
                "approverTypes", List.of(
                        Map.of("value", "USER", "label", "指定成员"),
                        Map.of("value", "ROLE", "label", "角色"),
                        Map.of("value", "DEPT_HEAD", "label", "部门主管（逐级）"),
                        Map.of("value", "DIRECT_LEADER", "label", "发起人直属上级"),
                        Map.of("value", "APPLICANT_SELF", "label", "发起人本人")),
                "conditionFields", List.of("amount", "category", "jobLevel"),
                "conditionOps", List.of("==", "!=", ">", ">=", "<", "<=")));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody FlowCreateDTO dto) {
        return R.ok(flowService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FlowCreateDTO dto) {
        flowService.update(id, dto);
        return R.ok();
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
