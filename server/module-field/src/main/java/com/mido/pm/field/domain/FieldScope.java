package com.mido.pm.field.domain;

import java.util.Arrays;

/** 自定义字段作用域。与 AuditActions 实体类型一致（task/project）。 */
public enum FieldScope {
    TASK("task", "task.updated"),
    PROJECT("project", "project.updated");

    private final String code;
    /** 字段值变更发布的领域事件名（取自 docs/domain-events.md，禁自造）。 */
    private final String updatedEvent;

    FieldScope(String code, String updatedEvent) {
        this.code = code;
        this.updatedEvent = updatedEvent;
    }

    public String getCode() {
        return code;
    }

    public String getUpdatedEvent() {
        return updatedEvent;
    }

    /** 解析作用域；非法返回 null。 */
    public static FieldScope fromCode(String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(null);
    }
}
