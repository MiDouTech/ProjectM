package com.mido.pm.verify.dto;

import java.math.BigDecimal;

/** NPSS 评价主体模板视图（租户级）。 */
public record SubjectTemplateVO(
        Long id,
        String name,
        BigDecimal weight,
        boolean beneficiary,
        Integer sort,
        boolean enabled) {
}
