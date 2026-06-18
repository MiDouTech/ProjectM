package com.mido.pm.project.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NPSS 适用默认策略单测：仅 O·定向整改/专项督办 默认不走 NPSS，其余默认走。
 */
class NpssPolicyTest {

    @Test
    void strategicAndInnovationDefaultRequireNpss() {
        assertTrue(NpssPolicy.defaultRequiresNpss("S", null));
        assertTrue(NpssPolicy.defaultRequiresNpss("I", null));
    }

    @Test
    void operationNormalDefaultRequiresNpss() {
        assertTrue(NpssPolicy.defaultRequiresNpss("O", "常规运营"));
        assertTrue(NpssPolicy.defaultRequiresNpss("O", null));
    }

    @Test
    void operationRectifyAndSuperviseDefaultNoNpss() {
        assertFalse(NpssPolicy.defaultRequiresNpss("O", "定向整改"));
        assertFalse(NpssPolicy.defaultRequiresNpss("O", "专项督办"));
    }

    @Test
    void rectifySubCategoryOnlyAppliesUnderOperation() {
        // 子类同名但类型非 O 时不豁免（防御性）
        assertTrue(NpssPolicy.defaultRequiresNpss("S", "定向整改"));
    }
}
