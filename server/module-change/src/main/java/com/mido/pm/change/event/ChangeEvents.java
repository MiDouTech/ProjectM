package com.mido.pm.change.event;

/** 变更域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class ChangeEvents {

    public static final String REQUESTED = "change.requested";   // 提交变更单
    public static final String APPLIED = "change.applied";       // 变更生效（回写被改实体）
    public static final String REJECTED = "change.rejected";     // 驳回/撤回，未生效

    private ChangeEvents() {
    }
}
