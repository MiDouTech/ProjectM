package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 职级节点 guard 单测（npss-rule §8）。
 */
class JobLevelNodeGuardTest {

    private final JobLevelNodeGuard guard = new JobLevelNodeGuard();
    private final FlowNode node = new FlowNode("lead", "负责人", List.of(1L), "or", "JOB_LEVEL", List.of(), null);

    private ApprovalContext ctx(String category, String jobLevel) {
        return new ApprovalContext(Map.of("category", category, "jobLevel", jobLevel));
    }

    @Test
    void strategicRejectsBelowL3() {
        assertThrows(BizException.class, () -> guard.check(node, ctx("S", "L2")));
        assertDoesNotThrow(() -> guard.check(node, ctx("S", "L3")));
    }

    @Test
    void operationalRejectsBelowL2() {
        assertThrows(BizException.class, () -> guard.check(node, ctx("O", "L1")));
        assertDoesNotThrow(() -> guard.check(node, ctx("O", "L2")));
    }

    @Test
    void innovationUnrestricted() {
        assertDoesNotThrow(() -> guard.check(node, ctx("I", "L1")));
    }
}
