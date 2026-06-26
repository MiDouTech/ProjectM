package com.mido.pm.verify.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.verify.domain.SubjectWeightValidator.SubjectWeight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * NPSS 评价主体权重硬校验（npss-rule §4，主体口径）：合计=100%、受益方≥50%。
 */
class SubjectWeightValidatorTest {

    private static SubjectWeight sw(String weight, boolean beneficiary) {
        return new SubjectWeight(new BigDecimal(weight), beneficiary);
    }

    @Test
    void passesWhenSum100AndBeneficiaryGe50() {
        // 受益方 30+30=60 ≥50，合计 100
        assertDoesNotThrow(() -> SubjectWeightValidator.validate(List.of(
                sw("30", true), sw("30", true), sw("20", false), sw("20", false))));
    }

    @Test
    void rejectsWhenSumNot100() {
        assertThrows(BizException.class, () -> SubjectWeightValidator.validate(List.of(
                sw("30", true), sw("30", true), sw("20", false))));
    }

    @Test
    void rejectsWhenBeneficiaryLt50() {
        // 受益方仅 40 <50（合计仍 100）
        assertThrows(BizException.class, () -> SubjectWeightValidator.validate(List.of(
                sw("40", true), sw("60", false))));
    }

    @Test
    void rejectsWhenEmpty() {
        assertThrows(BizException.class, () -> SubjectWeightValidator.validate(List.of()));
    }
}
