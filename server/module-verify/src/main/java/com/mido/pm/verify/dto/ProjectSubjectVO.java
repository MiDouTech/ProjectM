package com.mido.pm.verify.dto;

import java.math.BigDecimal;
import java.util.List;

/** NPSS 项目级评价主体视图（含成员干系人 id）。 */
public record ProjectSubjectVO(
        Long id,
        Long projectId,
        String name,
        BigDecimal weight,
        boolean beneficiary,
        Integer sort,
        List<Long> memberStakeholderIds) {
}
