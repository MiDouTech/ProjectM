package com.mido.pm.common.quota;

/**
 * 配额资源标识集中登记（与 sys_plan_quota.resource、用量统计 resource 对齐，禁自造）。
 */
public final class QuotaResources {

    /** 成员数 */
    public static final String USER = "user";
    /** 项目数 */
    public static final String PROJECT = "project";
    /** 任务数 */
    public static final String TASK = "task";
    /** 附件存储(MB) */
    public static final String STORAGE_MB = "storage_mb";

    /** 全部可统计资源（用量视图按此顺序展示）。 */
    public static final java.util.List<String> ALL = java.util.List.of(USER, PROJECT, TASK, STORAGE_MB);

    private QuotaResources() {
    }
}
