package com.mido.pm.stakeholder.domain;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * NPSS 权重硬校验单测（npss-rule §4）：①权重和=100% ②受益方≥50%。
 */
class WeightValidatorTest {

    private RoleWeight rw(String role, String weight) {
        return new RoleWeight(role, new BigDecimal(weight));
    }

    @Test
    void rejectsWhenSumNot100() {
        // 和=90，不足 100%
        List<RoleWeight> weights = List.of(
                rw("sponsor", "30"), rw("business", "30"), rw("team", "10"), rw("finance", "20"));
        assertThrows(BizException.class, () -> WeightValidator.validate(weights));
    }

    @Test
    void rejectsWhenBeneficiaryBelow50() {
        // 和=100 但受益方(sponsor+business)=40 < 50
        List<RoleWeight> weights = List.of(
                rw("sponsor", "20"), rw("business", "20"), rw("team", "30"), rw("other", "30"));
        assertThrows(BizException.class, () -> WeightValidator.validate(weights));
    }

    @Test
    void passesWhenValid() {
        List<RoleWeight> weights = List.of(
                rw("sponsor", "30"), rw("business", "30"), rw("team", "10"),
                rw("finance", "10"), rw("other", "20"));
        assertDoesNotThrow(() -> WeightValidator.validate(weights));
    }

    @Test
    void passesAtBoundary() {
        // 受益方=50 恰好达标，和=100
        List<RoleWeight> weights = List.of(
                rw("business", "50"), rw("team", "10"), rw("finance", "10"), rw("other", "30"));
        assertDoesNotThrow(() -> WeightValidator.validate(weights));
    }
}
