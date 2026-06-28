package com.mido.pm.view.domain;

import java.util.List;
import java.util.Map;

/**
 * 内置工作区组件库 + 各一级模块的默认导航（ADR-0003 · L1）。
 * 阶段一由代码内置（自定义组件随 L3 开放）；租户未编排时回落本默认（fail-safe）。
 * 默认清单按当前前端实有页面抽取，为「初稿待评审」——可由租户在管理后台增删/排序/改名。
 */
public final class WorkspaceCatalog {

    /** 工作区组件定义：code 唯一、module 归属、name/icon 默认、route 前端挂载点、pageType 页面类型。 */
    public record ComponentDef(String code, String module, String name, String icon,
                               String route, String pageType) {
    }

    /** 各模块可选组件（catalog）。key=module。 */
    private static final Map<String, List<ComponentDef>> CATALOG = Map.of(
            "project", List.of(
                    new ComponentDef("projects", "project", "全部项目", "Folder", "/project", "list"),
                    new ComponentDef("portfolios", "project", "项目集", "Files", "/project/portfolios", "list")),
            "goal", List.of(
                    new ComponentDef("goals", "goal", "目标", "Aim", "/goal", "list")),
            "approval", List.of(
                    new ComponentDef("approval-all", "approval", "全部", "Stamp", "/approval", "list"),
                    new ComponentDef("change-ledger", "approval", "变更台账", "Tickets", "/approval?tab=change", "list")),
            "report", List.of(
                    new ComponentDef("report", "report", "报表", "DataAnalysis", "/report", "custom")),
            "doc", List.of(
                    new ComponentDef("doc-center", "doc", "文档中心", "Document", "/doc", "custom")),
            "calendar", List.of(
                    new ComponentDef("calendar", "calendar", "日历", "Calendar", "/calendar", "custom")),
            "briefing", List.of(
                    new ComponentDef("briefing-all", "briefing", "全部", "Notebook", "/briefing", "list"),
                    new ComponentDef("briefing-submit", "briefing", "提交简报", "Document", "/briefing?tab=submit", "list"),
                    new ComponentDef("briefing-review", "briefing", "我评审的", "Stamp", "/briefing?tab=review", "list"),
                    new ComponentDef("briefing-members", "briefing", "成员简报", "Promotion", "/briefing?tab=members", "list"),
                    new ComponentDef("briefing-issues", "briefing", "跟进的问题", "Tickets", "/briefing?tab=issues", "list"),
                    new ComponentDef("briefing-stats", "briefing", "简报统计", "DataAnalysis", "/briefing?tab=stats", "list")));

    /** 默认导航顺序（=catalog 顺序，全部启用）。 */
    public static List<ComponentDef> catalog(String module) {
        return CATALOG.getOrDefault(module, List.of());
    }

    /** 按 code 查组件（用于编排回显 name/icon/route 回落）。 */
    public static ComponentDef find(String module, String code) {
        return catalog(module).stream().filter(c -> c.code().equals(code)).findFirst().orElse(null);
    }

    /** 已知一级模块集合。 */
    public static boolean knownModule(String module) {
        return CATALOG.containsKey(module);
    }

    private WorkspaceCatalog() {
    }
}
