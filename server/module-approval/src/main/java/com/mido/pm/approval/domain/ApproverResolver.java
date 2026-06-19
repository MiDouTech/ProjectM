package com.mido.pm.approval.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 审批人解析：把节点的「审批人类型 + 参数」解析为具体用户 ID（行业通用动态审批人）。
 * USER 型直接取节点 approvers；ROLE/部门主管/直属上级/发起人本人 经 {@link ApproverDirectory} 解析。
 * 结果去重、剔除 null；空结果由调用方决定（当前保持节点待定，不误判通过）。
 */
@Component
public class ApproverResolver {

    private final ApproverDirectory directory;

    public ApproverResolver(ApproverDirectory directory) {
        this.directory = directory;
    }

    /** 解析节点审批人具体用户 ID（applicantId 为发起人，供直属上级/部门主管/本人解析）。 */
    public List<Long> resolve(FlowNode node, Long applicantId) {
        Set<Long> out = new LinkedHashSet<>();
        switch (node.resolvedApproverType()) {
            case USER -> addAll(out, node.approvers());
            case ROLE -> {
                for (Long roleId : safe(node.approverValues())) {
                    if (roleId != null) {
                        addAll(out, directory.usersByRole(roleId));
                    }
                }
            }
            case DIRECT_LEADER -> add(out, directory.deptLeaderOf(applicantId, 1));
            case DEPT_HEAD -> add(out, directory.deptLeaderOf(applicantId, levelsUp(node)));
            case APPLICANT_SELF -> add(out, applicantId);
            default -> {
            }
        }
        return new ArrayList<>(out);
    }

    /** DEPT_HEAD 向上层级数：approverValues 首值，缺省 1，下限 1。 */
    private int levelsUp(FlowNode node) {
        List<Long> vals = node.approverValues();
        if (vals != null && !vals.isEmpty() && vals.get(0) != null && vals.get(0) > 0) {
            return vals.get(0).intValue();
        }
        return 1;
    }

    private void add(Set<Long> out, Long id) {
        if (id != null) {
            out.add(id);
        }
    }

    private void addAll(Set<Long> out, List<Long> ids) {
        if (ids != null) {
            for (Long id : ids) {
                add(out, id);
            }
        }
    }

    private List<Long> safe(List<Long> list) {
        return list == null ? List.of() : list;
    }
}
