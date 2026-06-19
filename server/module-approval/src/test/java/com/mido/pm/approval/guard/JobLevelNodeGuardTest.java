package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 职级节点 guard 单测（门槛制）：门槛 minJobLevel 由项目类型提供，空=不限。
 */
class JobLevelNodeGuardTest {

    private final JobLevelNodeGuard guard = new JobLevelNodeGuard();
    private final FlowNode node = new FlowNode("lead", "负责人", List.of(1L), "or", "JOB_LEVEL", List.of(), null);

    private ApprovalContext ctx(String minJobLevel, String jobLevel) {
        Map<String, Object> m = new HashMap<>();
        m.put("minJobLevel", minJobLevel);
        m.put("jobLevel", jobLevel);
        return new ApprovalContext(m);
    }

    @Test
    void rejectsBelowThreshold() {
        assertThrows(BizException.class, () -> guard.check(node, ctx("L3", "L2")));
        assertDoesNotThrow(() -> guard.check(node, ctx("L3", "L3")));
    }

    @Test
    void operationalThresholdL2() {
        assertThrows(BizException.class, () -> guard.check(node, ctx("L2", "L1")));
        assertDoesNotThrow(() -> guard.check(node, ctx("L2", "L2")));
    }

    @Test
    void emptyThresholdUnrestricted() {
        assertDoesNotThrow(() -> guard.check(node, ctx(null, "L1")));
    }
}
