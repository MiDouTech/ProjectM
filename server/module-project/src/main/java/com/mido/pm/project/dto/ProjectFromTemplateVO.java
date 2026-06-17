package com.mido.pm.project.dto;

import com.mido.pm.project.template.TemplateConfig;

/** 按模板建项目返回：项目 ID + 解析出的待供给骨架计划。 */
public record ProjectFromTemplateVO(Long projectId, TemplateConfig skeleton) {
}
