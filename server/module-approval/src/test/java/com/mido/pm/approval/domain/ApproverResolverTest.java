package com.mido.pm.approval.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批人解析单测：指定成员/角色/部门主管(逐级)/直属上级/发起人本人 + 去重 + 空结果。
 */
class ApproverResolverTest {

    /** 可编排的假目录：角色→成员表；部门主管按层级表。 */
    private ApproverResolver resolver(java.util.Map<Long, List<Long>> byRole,
                                      java.util.Map<Integer, Long> byLevel) {
        return new ApproverResolver(new ApproverDirectory() {
            @Override
            public List<Long> usersByRole(Long roleId) {
                return byRole.getOrDefault(roleId, List.of());
            }

            @Override
            public Long deptLeaderOf(Long applicantId, int levelsUp) {
                return byLevel.get(levelsUp);
            }
        });
    }

    private FlowNode node(String approverType, List<Long> approvers, List<Long> approverValues) {
        return new FlowNode("n", "节点", approvers, "or", null, List.of(), null, approverType, approverValues);
    }

    @Test
    void userTypeReturnsApproversDeduped() {
        var r = resolver(java.util.Map.of(), java.util.Map.of());
        assertEquals(List.of(10L, 20L), r.resolve(node("USER", List.of(10L, 20L, 10L), null), 1L));
    }

    @Test
    void legacyNodeWithoutTypeTreatedAsUser() {
        var r = resolver(java.util.Map.of(), java.util.Map.of());
        // 旧 7 参构造（无 approverType）
        FlowNode legacy = new FlowNode("n", "节点", List.of(99L), "or", null, List.of(), null);
        assertEquals(List.of(99L), r.resolve(legacy, 1L));
    }

    @Test
    void roleTypeExpandsToRoleMembers() {
        var r = resolver(java.util.Map.of(5L, List.of(100L, 200L), 6L, List.of(200L, 300L)),
                java.util.Map.of());
        assertEquals(List.of(100L, 200L, 300L),
                r.resolve(node("ROLE", null, List.of(5L, 6L)), 1L));
    }

    @Test
    void directLeaderResolvesLevelOne() {
        var r = resolver(java.util.Map.of(), java.util.Map.of(1, 70L));
        assertEquals(List.of(70L), r.resolve(node("DIRECT_LEADER", null, null), 42L));
    }

    @Test
    void deptHeadResolvesGivenLevel() {
        var r = resolver(java.util.Map.of(), java.util.Map.of(1, 70L, 2, 80L));
        assertEquals(List.of(80L), r.resolve(node("DEPT_HEAD", null, List.of(2L)), 42L));
        // 缺省层级=1
        assertEquals(List.of(70L), r.resolve(node("DEPT_HEAD", null, null), 42L));
    }

    @Test
    void applicantSelfResolvesToApplicant() {
        var r = resolver(java.util.Map.of(), java.util.Map.of());
        assertEquals(List.of(42L), r.resolve(node("APPLICANT_SELF", null, null), 42L));
    }

    @Test
    void emptyWhenDirectoryHasNoMatch() {
        var r = resolver(java.util.Map.of(), java.util.Map.of());
        assertTrue(r.resolve(node("DIRECT_LEADER", null, null), 42L).isEmpty());
        assertTrue(r.resolve(node("ROLE", null, List.of(9L)), 42L).isEmpty());
    }
}
