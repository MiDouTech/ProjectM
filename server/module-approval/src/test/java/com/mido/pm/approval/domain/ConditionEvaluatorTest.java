package com.mido.pm.approval.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件路由单测：金额/项目类型/职级三类比较。
 */
class ConditionEvaluatorTest {

    private ApprovalContext ctx(Map<String, Object> m) {
        return new ApprovalContext(m);
    }

    @Test
    void amountGreaterThan() {
        NodeCondition c = new NodeCondition("amount", ">", "100000");
        assertTrue(ConditionEvaluator.evaluate(c, ctx(Map.of("amount", 200000))));
        assertFalse(ConditionEvaluator.evaluate(c, ctx(Map.of("amount", 50000))));
    }

    @Test
    void categoryEquals() {
        NodeCondition c = new NodeCondition("category", "==", "S");
        assertTrue(ConditionEvaluator.evaluate(c, ctx(Map.of("category", "S"))));
        assertFalse(ConditionEvaluator.evaluate(c, ctx(Map.of("category", "O"))));
    }

    @Test
    void jobLevelGreaterEqual() {
        NodeCondition c = new NodeCondition("jobLevel", ">=", "L3");
        assertTrue(ConditionEvaluator.evaluate(c, ctx(Map.of("jobLevel", "L3"))));
        assertFalse(ConditionEvaluator.evaluate(c, ctx(Map.of("jobLevel", "L2"))));
    }

    @Test
    void nullConditionAlwaysTrue() {
        assertTrue(ConditionEvaluator.evaluate(null, ctx(Map.of())));
    }
}
