package com.mido.pm.cost.domain;

/** 费用状态字典（pm_cost.status，存中文，与前端字典一致）。 */
public final class CostStatus {

    /** 未发生（默认，预算/计划态） */
    public static final String NOT_OCCURRED = "未发生";
    /** 已发生（审批通过 / 实际发生） */
    public static final String OCCURRED = "已发生";
    /** 被退回（审批驳回） */
    public static final String RETURNED = "被退回";

    private CostStatus() {
    }
}
