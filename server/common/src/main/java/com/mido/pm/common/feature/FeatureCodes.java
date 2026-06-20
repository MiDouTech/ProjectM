package com.mido.pm.common.feature;

import java.util.List;

/**
 * 可按套餐下发的功能开关码集中登记（平台配 plan→feature，租户据生效套餐查询启用项做前端门控）。
 * 新增功能开关前在此查重；与前端门控约定保持一致。
 */
public final class FeatureCodes {

    /** 甘特图 */
    public static final String GANTT = "gantt";
    /** 目标/OKR */
    public static final String OKR = "okr";
    /** NPSS 价值验收 */
    public static final String NPSS = "npss";
    /** 文档 */
    public static final String DOC = "doc";
    /** 费用 */
    public static final String COST = "cost";
    /** 报表/度量 */
    public static final String REPORT = "report";
    /** 变更中心 */
    public static final String CHANGE = "change";
    /** 开放平台（API Key） */
    public static final String OPENAPI = "openapi";

    /** 全部可配功能开关码。 */
    public static final List<String> ALL = List.of(GANTT, OKR, NPSS, DOC, COST, REPORT, CHANGE, OPENAPI);

    private FeatureCodes() {
    }
}
