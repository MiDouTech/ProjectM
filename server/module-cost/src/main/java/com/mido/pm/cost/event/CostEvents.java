package com.mido.pm.cost.event;

/** 费用域领域事件名（取自 docs/domain-events.md，不得自造）。 */
public final class CostEvents {

    /** 提报费用（发起审批） */
    public static final String SUBMITTED = "cost.submitted";
    /** 费用累计超预算 */
    public static final String EXCEEDED_BUDGET = "cost.exceeded.budget";
    /** 项目实际成本 > 预算（项目域口径，由费用累计触发） */
    public static final String PROJECT_BUDGET_EXCEEDED = "project.budget.exceeded";

    private CostEvents() {
    }
}
