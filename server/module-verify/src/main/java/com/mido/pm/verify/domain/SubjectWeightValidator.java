package com.mido.pm.verify.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * NPSS 评价主体权重硬校验（npss-rule §4，主体口径），纯函数、可单测：
 * A. 受益方主体权重合计 ≥ 50%；
 * B. 全部启用主体权重之和 = 100%（允许 ±0.01 浮点误差）。
 * 调用方只传入"启用"主体；受益方由主体自身 beneficiary 标记决定。
 */
public final class SubjectWeightValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FIFTY = new BigDecimal("50");
    private static final BigDecimal EPS = new BigDecimal("0.01");

    /** 一个评价主体的权重与受益方标记。 */
    public record SubjectWeight(BigDecimal weight, boolean beneficiary) {
    }

    private SubjectWeightValidator() {
    }

    public static void validate(List<SubjectWeight> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "评价主体不能为空");
        }
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal beneficiary = BigDecimal.ZERO;
        for (SubjectWeight s : subjects) {
            BigDecimal v = s.weight() == null ? BigDecimal.ZERO : s.weight();
            sum = sum.add(v);
            if (s.beneficiary()) {
                beneficiary = beneficiary.add(v);
            }
        }
        // B. 权重和 = 100%
        if (sum.subtract(HUNDRED).abs().compareTo(EPS) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "全部评价主体权重之和须为 100%，当前 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        // A. 受益方合计 ≥ 50%
        if (beneficiary.compareTo(FIFTY) < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "受益方主体权重合计须≥50%，当前 " + beneficiary.stripTrailingZeros().toPlainString() + "%");
        }
    }
}
