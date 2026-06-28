package com.mido.pm.verify.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * NPSS 项目级评价主体视图。主体名称/权重/受益方取自租户模板（templateId 对应），项目内只读；
 * 成员（干系人 id）为项目级、可编辑。
 */
public record ProjectSubjectVO(
        Long id,
        Long projectId,
        Long templateId,
        String name,
        BigDecimal weight,
        boolean beneficiary,
        Integer sort,
        List<Long> memberStakeholderIds) {
}
