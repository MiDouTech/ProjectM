package com.mido.pm.stakeholder.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * NPSS 权重硬校验（npss-rule §4），纯函数、可单测：
 * A. 受益方(发起人+业务方)权重合计 ≥ 50%；
 * B. 全部权重之和 = 100%（允许 ±0.01 浮点误差）。
 */
public final class WeightValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FIFTY = new BigDecimal("50");
    private static final BigDecimal EPS = new BigDecimal("0.01");

    private WeightValidator() {
    }

    public static void validate(List<RoleWeight> weights) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal beneficiary = BigDecimal.ZERO;
        for (RoleWeight w : weights) {
            BigDecimal v = w.weight() == null ? BigDecimal.ZERO : w.weight();
            sum = sum.add(v);
            if (StakeholderRole.isBeneficiaryRole(w.role())) {
                beneficiary = beneficiary.add(v);
            }
        }
        // B. 权重和 = 100%
        if (sum.subtract(HUNDRED).abs().compareTo(EPS) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "全部干系人权重之和须为 100%，当前 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        // A. 受益方合计 ≥ 50%
        if (beneficiary.compareTo(FIFTY) < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "受益方(发起人+业务方)权重合计须≥50%，当前 "
                            + beneficiary.stripTrailingZeros().toPlainString() + "%");
        }
    }
}
