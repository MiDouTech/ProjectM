package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 立项职级规则单测（npss-rule §8），跨域共用的单一事实源。
 */
class JobLevelRuleTest {

    @Test
    void requiredLevels() {
        assertEquals(3, JobLevelRule.requiredLevel("S"));
        assertEquals(2, JobLevelRule.requiredLevel("O"));
        assertEquals(0, JobLevelRule.requiredLevel("I"));
        assertEquals(0, JobLevelRule.requiredLevel(null));
    }

    @Test
    void parseLevel() {
        assertEquals(3, JobLevelRule.parseLevel("L3"));
        assertEquals(2, JobLevelRule.parseLevel("l2"));
        assertEquals(0, JobLevelRule.parseLevel(null));
        assertEquals(0, JobLevelRule.parseLevel("x"));
    }

    @Test
    void qualifies() {
        assertTrue(JobLevelRule.qualifies("S", "L3"));
        assertFalse(JobLevelRule.qualifies("S", "L2"));
        assertTrue(JobLevelRule.qualifies("O", "L2"));
        assertFalse(JobLevelRule.qualifies("O", "L1"));
        assertTrue(JobLevelRule.qualifies("I", null));
    }

    @Test
    void assertQualified() {
        assertThrows(BizException.class, () -> JobLevelRule.assertQualified("S", "L2"));
        assertDoesNotThrow(() -> JobLevelRule.assertQualified("S", "L3"));
        assertThrows(BizException.class, () -> JobLevelRule.assertQualified("S", null));
    }
}
