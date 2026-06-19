package com.mido.pm.goal.event;

/**
 * 目标域领域事件名（取自 docs/domain-events.md，集中登记，禁自造）。
 */
public final class GoalEvents {

    public static final String CREATED = "goal.created";
    public static final String UPDATED = "goal.updated";
    public static final String DELETED = "goal.deleted";
    /** KR 量化进度变化（手动改当前值 / 项目进度自动汇总反写） */
    public static final String PROGRESS_CHANGED = "goal.progress.changed";
    public static final String ALIGNED = "goal.aligned";
    public static final String UNALIGNED = "goal.unaligned";

    private GoalEvents() {
    }
}
