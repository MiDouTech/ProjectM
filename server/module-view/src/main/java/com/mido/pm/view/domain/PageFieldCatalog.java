package com.mido.pm.view.domain;

import java.util.List;
import java.util.Map;

/**
 * 内置字段目录（ADR-0004 · L3）：按实体 target 声明可纳入页面编排的内置字段。
 * 前端将其与 pm_field_def 自定义字段合成为统一字段集；type 对齐 module-field
 * （text/number/date/select/multi_select/checkbox/user）。
 */
public final class PageFieldCatalog {

    /** 内置字段定义：key 为实体属性名（前端按此绑定）；required 为内置默认必填。 */
    public record FieldDef(String key, String label, String type, boolean required) {
    }

    private static final Map<String, List<FieldDef>> CATALOG = Map.of(
            "task", List.of(
                    new FieldDef("title", "标题", "text", true),
                    new FieldDef("description", "描述", "text", false),
                    new FieldDef("assigneeId", "负责人", "user", false),
                    new FieldDef("priority", "优先级", "select", false),
                    new FieldDef("stage", "阶段", "text", false),
                    new FieldDef("startDate", "开始时间", "date", false),
                    new FieldDef("dueDate", "截止时间", "date", false),
                    new FieldDef("isMilestone", "里程碑", "checkbox", false),
                    new FieldDef("estHours", "预估工时", "number", false)),
            "project", List.of(
                    new FieldDef("name", "项目名", "text", true),
                    new FieldDef("description", "描述", "text", false),
                    new FieldDef("leaderId", "负责人", "user", false),
                    new FieldDef("budget", "预算", "number", false),
                    new FieldDef("startDate", "开始时间", "date", false),
                    new FieldDef("endDate", "结束时间", "date", false)));

    public static List<FieldDef> of(String target) {
        return CATALOG.getOrDefault(target, List.of());
    }

    public static boolean known(String target) {
        return CATALOG.containsKey(target);
    }

    private PageFieldCatalog() {
    }
}
