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
    /** 删除实体（detail 可含被删实体快照） */
    public static final String DELETED = "deleted";
    /** 状态流转（detail = {from,to}） */
    public static final String STATUS_CHANGED = "status_changed";
    /** 归档/恢复（detail = {archived}） */
    public static final String ARCHIVED = "archived";
    /** 指派/改派（detail = {from,to}，值为用户ID） */
    public static final String ASSIGNED = "assigned";
    /** MCP 工具调用（detail = {tool, scope, outcome}） */
    public static final String MCP_INVOKE = "mcp_invoke";

    // ===== 管理操作（账号权限 / 成员组织 / 配置）=====
    /** 角色权限码变更（detail = {from:[...], to:[...]}） */
    public static final String PERMS_CHANGED = "perms_changed";
    /** 角色数据范围变更（detail = {from:[...], to:[...]}） */
    public static final String DATA_SCOPE_CHANGED = "data_scope_changed";
    /** 角色字段权限变更（detail = {from:[...], to:[...]}） */
    public static final String FIELD_PERM_CHANGED = "field_perm_changed";
    /** 给用户分配角色（detail = {from:[roleId...], to:[roleId...]}） */
    public static final String ROLES_ASSIGNED = "roles_assigned";
    /** 添加项目成员（detail = {userId, projectRole}） */
    public static final String MEMBER_ADDED = "member_added";
    /** 移除项目成员（detail = {userId, projectRole}） */
    public static final String MEMBER_REMOVED = "member_removed";

    // ===== 功能模块（管理后台分组过滤）=====
    /** 模块：账号权限（角色/权限码/数据范围/字段权限/分配角色） */
    public static final String MODULE_PERMISSION = "permission";
    /** 模块：成员与组织（用户/部门/项目成员） */
    public static final String MODULE_MEMBER = "member";
    /** 模块：配置（项目类型/自定义字段/工作流等） */
    public static final String MODULE_CONFIG = "config";
    /** 模块：项目 */
    public static final String MODULE_PROJECT = "project";
    /** 模块：任务 */
    public static final String MODULE_TASK = "task";
    /** 模块：MCP/开放平台 */
    public static final String MODULE_MCP = "mcp";

    /** 实体类型：项目 */
    public static final String TARGET_PROJECT = "project";
    /** 实体类型：任务 */
    public static final String TARGET_TASK = "task";
    /** 实体类型：MCP 连接器（entityId = API Key 主键） */
    public static final String TARGET_MCP = "mcp";
    /** 实体类型：角色 */
    public static final String TARGET_ROLE = "role";
    /** 实体类型：用户 */
    public static final String TARGET_USER = "user";
    /** 实体类型：部门 */
    public static final String TARGET_DEPT = "dept";
    /** 实体类型：项目成员（entityId = 项目 ID） */
    public static final String TARGET_PROJECT_MEMBER = "project_member";
    /** 实体类型：项目类型 */
    public static final String TARGET_PROJECT_TYPE = "project_type";
    /** 实体类型：状态库状态 */
    public static final String TARGET_STATUS = "status";
    /** 实体类型：工作项类型 */
    public static final String TARGET_WORK_ITEM_TYPE = "work_item_type";
    /** 实体类型：优先级模式 */
    public static final String TARGET_PRIORITY_MODE = "priority_mode";
    /** 实体类型：数据源（选项集） */
    public static final String TARGET_DATA_SOURCE = "data_source";
    /** 实体类型：项目角色 */
    public static final String TARGET_PROJECT_ROLE = "project_role";
    /** 实体类型：关联类型定义 */
    public static final String TARGET_RELATION_DEF = "relation_def";
    /** 实体类型：工作项关联实例（entityId = 源任务 ID） */
    public static final String TARGET_RELATION = "relation";
    /** 实体类型：项目组件安装（entityId = 项目 ID） */
    public static final String TARGET_COMPONENT = "component";
    /** 实体类型：项目集（组合） */
    public static final String TARGET_PORTFOLIO = "portfolio";

    private AuditActions() {
    }
}
