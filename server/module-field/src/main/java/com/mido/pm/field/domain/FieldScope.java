package com.mido.pm.field.domain;

import java.util.Arrays;

/** 自定义字段作用域。与 AuditActions 实体类型一致（task/project）。 */
public enum FieldScope {
    TASK("task"),
    PROJECT("project");

    private final String code;

    FieldScope(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 解析作用域；非法返回 null。 */
    public static FieldScope fromCode(String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(null);
    }
}
