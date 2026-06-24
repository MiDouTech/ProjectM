package com.mido.pm.common.audit;

/**
 * 审计动作码（活动流）。集中登记，禁自造。
 * 注意：这是审计日志的动作码，与 docs/domain-events.md 的领域事件名是两套体系，互不替代。
 */
public final class AuditActions {

    /** 创建实体 */
    public static final String CREATED = "created";
    /** 字段编辑（detail.changes = [{field,from,to}]） */
    public static final String UPDATED = "updated";
    /** 状态流转（detail = {from,to}） */
    public static final String STATUS_CHANGED = "status_changed";
    /** 归档/恢复（detail = {archived}） */
    public static final String ARCHIVED = "archived";
    /** 指派/改派（detail = {from,to}，值为用户ID） */
    public static final String ASSIGNED = "assigned";
    /** MCP 工具调用（detail = {tool, scope, outcome}） */
    public static final String MCP_INVOKE = "mcp_invoke";

    /** 实体类型：项目 */
    public static final String TARGET_PROJECT = "project";
    /** 实体类型：任务 */
    public static final String TARGET_TASK = "task";
    /** 实体类型：MCP 连接器（entityId = API Key 主键） */
    public static final String TARGET_MCP = "mcp";

    private AuditActions() {
    }
}
