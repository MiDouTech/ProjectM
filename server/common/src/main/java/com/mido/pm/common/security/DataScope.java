package com.mido.pm.common.security;

/**
 * 数据范围类型（对应 sys_role_data_scope.scope）。
 * 取值：self / dept / dept_and_sub / all / custom（见 docs/data-model.md 状态字典）。
 */
public enum DataScope {

    /** 仅本人 */
    SELF("self"),
    /** 本部门 */
    DEPT("dept"),
    /** 本部门及下属 */
    DEPT_AND_SUB("dept_and_sub"),
    /** 全部 */
    ALL("all"),
    /** 自定义部门集 */
    CUSTOM("custom");

    private final String code;

    DataScope(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 按 code 解析；未知值回落到给定默认，避免空指针。 */
    public static DataScope fromCode(String code, DataScope defaultScope) {
        for (DataScope s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return defaultScope;
    }

    /**
     * 取多个范围中权限最宽的一个（数值越大越宽）。用于一个用户多角色时合并。
     * 宽窄序：SELF &lt; DEPT &lt; DEPT_AND_SUB &lt; ALL；CUSTOM 视为与 DEPT 同级特例。
     */
    public int breadth() {
        return switch (this) {
            case SELF -> 1;
            case CUSTOM -> 2;
            case DEPT -> 2;
            case DEPT_AND_SUB -> 3;
            case ALL -> 4;
        };
    }
}
