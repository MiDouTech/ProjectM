package com.mido.pm.verify.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 结果验收结论展示对象（验收 Tab 用）。 */
public record ResultVerifyVO(
        Long id,
        Long projectId,
        String verdict,
        Boolean onTime,
        Boolean inBudget,
        Boolean inScope,
        BigDecimal completionRate,
        String remark,
        Long verifiedBy,
        LocalDateTime verifiedAt) {
}
