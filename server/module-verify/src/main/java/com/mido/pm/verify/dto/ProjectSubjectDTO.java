package com.mido.pm.verify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** NPSS 项目级评价主体项（整组提交）。成员即干系人 id；每个主体须≥1 成员，权重合计=100%。 */
public record ProjectSubjectDTO(
        Long id,
        @NotBlank(message = "评价主体名称不能为空") String name,
        @NotNull(message = "权重不能为空") BigDecimal weight,
        Boolean beneficiary,
        Integer sort,
        List<Long> memberStakeholderIds) {
}
