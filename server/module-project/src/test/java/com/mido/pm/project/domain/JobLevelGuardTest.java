package com.mido.pm.project.domain;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 立项职级 guard 单测（npss-rule §8）：S→L3+ / O→L2+ / I 不限。
 */
class JobLevelGuardTest {

    @Test
    void strategicRequiresL3() {
        assertThrows(BizException.class,
                () -> JobLevelGuard.assertLeaderQualified(ProjectCategory.S, "L2"));
        assertDoesNotThrow(() -> JobLevelGuard.assertLeaderQualified(ProjectCategory.S, "L3"));
        assertDoesNotThrow(() -> JobLevelGuard.assertLeaderQualified(ProjectCategory.S, "L4"));
    }

    @Test
    void operationalRequiresL2() {
        assertThrows(BizException.class,
                () -> JobLevelGuard.assertLeaderQualified(ProjectCategory.O, "L1"));
        assertDoesNotThrow(() -> JobLevelGuard.assertLeaderQualified(ProjectCategory.O, "L2"));
    }

    @Test
    void innovationUnrestricted() {
        assertDoesNotThrow(() -> JobLevelGuard.assertLeaderQualified(ProjectCategory.I, "L1"));
        assertDoesNotThrow(() -> JobLevelGuard.assertLeaderQualified(ProjectCategory.I, null));
    }

    @Test
    void missingLevelRejectedForStrategic() {
        assertThrows(BizException.class,
                () -> JobLevelGuard.assertLeaderQualified(ProjectCategory.S, null));
        assertThrows(BizException.class,
                () -> JobLevelGuard.assertLeaderQualified(ProjectCategory.S, ""));
    }
}
