package com.mido.pm.common.security;

/**
 * 字段级权限访问级别（对齐截图「仅查看 / 可编辑」两态）。
 * 未配置某字段时默认 EDIT（opt-in 收紧）；多角色合并时 EDIT 比 VIEW 宽。
 */
public final class FieldAccess {

    /** 仅查看（不可编辑） */
    public static final String VIEW = "view";
    /** 可编辑 */
    public static final String EDIT = "edit";

    public static boolean isValid(String access) {
        return VIEW.equals(access) || EDIT.equals(access);
    }

    private FieldAccess() {
    }
}
