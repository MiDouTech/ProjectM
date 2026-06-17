package com.mido.pm.project.dto;

import java.math.BigDecimal;

/**
 * 立项申请单（按 S/I/O 动态字段）。biz_type=project_init。
 * leaderId/budget 可在申请时覆盖项目已有值；提交后用于审批路由与职级 guard。
 */
public record InitiationFormDTO(
        String objective,
        BigDecimal budget,
        Long leaderId,
        String stakeholderDraft,
        String valueHypothesis) {
}
