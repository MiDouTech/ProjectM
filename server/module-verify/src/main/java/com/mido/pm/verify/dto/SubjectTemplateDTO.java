package com.mido.pm.verify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** NPSS 评价主体模板项（租户级，整组提交）。weight 为百分比；启用主体合计须=100%。 */
public record SubjectTemplateDTO(
        Long id,
        @NotBlank(message = "评价主体名称不能为空") String name,
        @NotNull(message = "权重不能为空") BigDecimal weight,
        Boolean beneficiary,
        Integer sort,
        Boolean enabled) {
}
