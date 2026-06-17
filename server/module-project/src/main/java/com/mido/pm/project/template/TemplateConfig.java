package com.mido.pm.project.template;

import java.util.List;

/**
 * 项目模板配置 schema（pm_project_template.config 的 JSON 结构，固定不自由发挥）。
 * 含：阶段划分(phases，每阶段带任务骨架)、默认干系人角色与权重(对齐 npss-rule §6)、
 * 默认审批流标识(approvalFlow)、默认验收方式(verifyMethod)。
 */
public record TemplateConfig(
        List<Phase> phases,
        List<StakeholderWeight> stakeholders,
        String approvalFlow,
        String verifyMethod) {

    /** 阶段及其任务骨架。 */
    public record Phase(String name, List<String> tasks) {
    }

    /** 默认干系人角色与权重（role: sponsor/business/team/finance/management/regulator/other）。 */
    public record StakeholderWeight(String role, Integer weight) {
    }
}
