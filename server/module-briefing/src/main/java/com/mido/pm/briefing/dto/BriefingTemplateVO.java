package com.mido.pm.briefing.dto;

import java.util.List;

/** 简报模板视图。fields 由 schema 解析，供前端渲染填报表单。 */
public record BriefingTemplateVO(
        Long id,
        String name,
        String type,
        List<FieldDefVO> fields,
        String status) {
}
