package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 立项职级规则单测（门槛制）：门槛由项目类型 min_job_level 提供，跨域共用的单一事实源。
 */
class JobLevelRuleTest {

    @Test
    void parseLevel() {
        assertEquals(3, JobLevelRule.parseLevel("L3"));
        assertEquals(2, JobLevelRule.parseLevel("l2"));
        assertEquals(0, JobLevelRule.parseLevel(null));
        assertEquals(0, JobLevelRule.parseLevel("x"));
    }

    @Test
    void qualifies() {
        assertTrue(JobLevelRule.qualifies("L3", "L3"));
        assertFalse(JobLevelRule.qualifies("L3", "L2"));
        assertTrue(JobLevelRule.qualifies("L2", "L2"));
        assertFalse(JobLevelRule.qualifies("L2", "L1"));
        // 门槛为空=不限，恒达标
        assertTrue(JobLevelRule.qualifies(null, null));
        assertTrue(JobLevelRule.qualifies(null, "L1"));
    }

    @Test
    void assertQualified() {
        assertThrows(BizException.class, () -> JobLevelRule.assertQualified("L3", "L2"));
        assertDoesNotThrow(() -> JobLevelRule.assertQualified("L3", "L3"));
        assertThrows(BizException.class, () -> JobLevelRule.assertQualified("L3", null));
        // 门槛为空不抛
        assertDoesNotThrow(() -> JobLevelRule.assertQualified(null, null));
    }
}
