package com.mido.pm.common.datascope;

/**
 * 数据范围资源键（对应 sys_role_data_scope.resource），集中登记避免散落字符串。
 * 用户(user)键在 module-org 内自管；此处登记跨域复用的项目/任务键。
 */
public final class ScopeResource {

    public static final String PROJECT = "project";
    public static final String TASK = "task";

    private ScopeResource() {
    }
}
