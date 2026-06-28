package com.mido.pm.verify.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * NPSS 项目级评价主体成员配置项（整组提交）。
 * 主体名称/权重/受益方为租户级统一配置（{@link SubjectTemplateDTO}），项目内只读、不在此提交；
 * 此处仅按租户模板主体（templateId）配置各自的成员（干系人）。每个启用主体须≥1 成员，干系人不可跨主体。
 */
public record ProjectSubjectDTO(
        @NotNull(message = "评价主体(模板)不能为空") Long templateId,
        Integer sort,
        List<Long> memberStakeholderIds) {
}
