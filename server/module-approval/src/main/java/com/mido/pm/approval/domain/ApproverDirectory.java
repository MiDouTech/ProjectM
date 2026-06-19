package com.mido.pm.approval.domain;

import java.util.List;

/**
 * 审批人目录端口（SPI）：动态审批人解析所需的组织能力，由组织域（module-org）实现并注入。
 * 审批引擎不直接依赖组织表，遵循跨域只经接口（CLAUDE.md §4）。
 */
public interface ApproverDirectory {

    /** 拥有指定角色的全部成员用户 ID（无则空）。 */
    List<Long> usersByRole(Long roleId);

    /**
     * 发起人向上第 levelsUp 级部门的负责人用户 ID。
     * levelsUp=1 为发起人所在部门负责人（直属上级）；逐级沿部门 parent 上溯。无则返回 null。
     */
    Long deptLeaderOf(Long applicantId, int levelsUp);
}
