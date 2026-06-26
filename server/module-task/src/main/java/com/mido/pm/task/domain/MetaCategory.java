package com.mido.pm.task.domain;

import java.util.Set;

/**
 * 状态元类别：所有业务状态归约到三类，用于完成率统计与"是否完成"判定（对标 Worktile）。
 */
public final class MetaCategory {

    public static final String NOT_STARTED = "未开始";
    public static final String IN_PROGRESS = "进行中";
    public static final String DONE = "已完成";

    private static final Set<String> ALL = Set.of(NOT_STARTED, IN_PROGRESS, DONE);

    public static boolean isValid(String category) {
        return ALL.contains(category);
    }

    private MetaCategory() {
    }
}
