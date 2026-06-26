import request from './request'

/** 认证（Step 1-2） */
export const authApi = {
  // tenantCode 可选（多租户登录隔离）：留空时后端回落自用租户，行为与原先一致。
  login: (data) => request.post('/auth/login', data),
  // 企微 SSO 授权地址：{ enabled, url }。redirectUri 为前端回调页。
  wecomAuthorizeUrl: (redirectUri) =>
    request.get('/auth/wecom/authorize-url', { params: { redirectUri } }),
  // 企微 SSO 登录：用授权 code 换令牌
  wecomLogin: (code) => request.post('/auth/wecom/login', { code }),
}

/** 用户/成员（Step 1-1） */
export const userApi = {
  query: (data) => request.post('/users/query', data),
  get: (id) => request.get(`/users/${id}`),
  create: (data) => request.post('/users', data),
  update: (id, data) => request.put(`/users/${id}`, data),
  remove: (id) => request.delete(`/users/${id}`),
  assignRoles: (id, roleIds) => request.put(`/users/${id}/roles`, { roleIds }),
  // 企微通讯录全量同步（部门/成员 → sys_dept/sys_user + sys_identity_map）
  syncWecomContacts: () => request.post('/wecom/contacts/sync'),
}

/**
 * 加载成员列表用于选人/名称解析（统一入口，集中 size 上限，避免各页散落魔法值与重复实现）。
 * 返回成员数组。
 */
export const fetchMembers = () =>
  userApi.query({ page: 1, size: 500 }).then((res) => res.list || [])

/** 角色 + 权限码 + 数据范围（Step 1-1） */
export const roleApi = {
  list: () => request.get('/roles'),
  create: (data) => request.post('/roles', data),
  update: (id, data) => request.put(`/roles/${id}`, data),
  remove: (id) => request.delete(`/roles/${id}`),
  getPerms: (id) => request.get(`/roles/${id}/perms`),
  savePerms: (id, permCodes) => request.put(`/roles/${id}/perms`, permCodes),
  getDataScopes: (id) => request.get(`/roles/${id}/data-scopes`),
  saveDataScopes: (id, settings) => request.put(`/roles/${id}/data-scopes`, settings),
  getFieldPerms: (id) => request.get(`/roles/${id}/field-perms`),
  saveFieldPerms: (id, settings) => request.put(`/roles/${id}/field-perms`, settings),
}

/**
 * 字段级权限（当前用户视角）：返回某资源下当前用户的只读字段键集合，供表单只读渲染。
 * 安全边界在后端（写入拦截），本接口仅 UX。
 */
export const fieldPermApi = {
  viewOnly: (resource) => request.get('/field-perms/view-only', { params: { resource } }),
}

/** 字段权限访问级别（对齐截图「仅查看/可编辑」） */
export const FIELD_ACCESS = [
  { value: 'edit', label: '可编辑' },
  { value: 'view', label: '仅查看' },
]

/**
 * 可配置字段权限的资源与字段清单（与后端字段键一致）。
 * 未在此登记的字段默认可编辑；新增受控字段时同步维护本表与后端 enforcement。
 */
export const FIELD_PERM_RESOURCES = [
  {
    value: 'task',
    label: '任务',
    fields: [
      { value: 'title', label: '标题' },
      { value: 'priority', label: '优先级' },
      { value: 'status', label: '状态' },
      { value: 'stage', label: '阶段' },
      { value: 'assignee', label: '负责人' },
      { value: 'startDate', label: '开始日期' },
      { value: 'dueDate', label: '截止日期' },
      { value: 'isMilestone', label: '里程碑' },
      { value: 'description', label: '描述' },
    ],
  },
  {
    value: 'project',
    label: '项目',
    fields: [
      { value: 'name', label: '名称' },
      { value: 'subCategory', label: '子类别' },
      { value: 'leaderId', label: '负责人' },
      { value: 'budget', label: '预算' },
      { value: 'description', label: '描述' },
      { value: 'startDate', label: '开始日期' },
      { value: 'endDate', label: '结束日期' },
    ],
  },
]

/**
 * 企业微信集成配置（租户自助）。secret 出参脱敏（仅 *Set 布尔位），保存时留空表示不修改原值。
 */
export const wecomConfigApi = {
  get: () => request.get('/wecom/config'),
  status: () => request.get('/wecom/config/status'),
  save: (data) => request.put('/wecom/config', data),
}

/** 部门树（Step 1-1） */
export const deptApi = {
  tree: () => request.get('/depts'),
  create: (data) => request.post('/depts', data),
  update: (id, data) => request.put(`/depts/${id}`, data),
  remove: (id) => request.delete(`/depts/${id}`),
}

/**
 * 开放平台 API Key（租户应用 /admin 下管理；走租户 token）。
 * keyPrefix/雪花 id 全程字符串；create 返回的 apiKey 为明文，仅此一次返回。
 */
export const apiKeyApi = {
  list: () => request.get('/apikeys'),
  create: (data) => request.post('/apikeys', data),
  revoke: (id) => request.put(`/apikeys/${id}/revoke`),
  remove: (id) => request.delete(`/apikeys/${id}`),
}

/**
 * 操作日志（租户管理后台，合规审计）。复杂过滤走 POST /query。
 * query 入参：{ userId, module, action, target, targetId, startTime, endTime, page, size }
 */
export const auditLogApi = {
  query: (data) => request.post('/audit-logs/query', data),
}

/** 操作日志功能模块字典（与后端 AuditActions.MODULE_* 一致） */
export const AUDIT_MODULES = [
  { value: 'permission', label: '账号权限' },
  { value: 'member', label: '成员组织' },
  { value: 'config', label: '配置' },
  { value: 'project', label: '项目' },
  { value: 'task', label: '任务' },
  { value: 'mcp', label: '开放平台' },
]

/** 操作日志动作码字典（与后端 AuditActions.* 一致） */
export const AUDIT_ACTIONS = [
  { value: 'created', label: '创建' },
  { value: 'updated', label: '编辑' },
  { value: 'deleted', label: '删除' },
  { value: 'status_changed', label: '状态变更' },
  { value: 'archived', label: '归档/恢复' },
  { value: 'assigned', label: '指派/改派' },
  { value: 'perms_changed', label: '权限码变更' },
  { value: 'data_scope_changed', label: '数据范围变更' },
  { value: 'field_perm_changed', label: '字段权限变更' },
  { value: 'roles_assigned', label: '分配角色' },
  { value: 'member_added', label: '添加成员' },
  { value: 'member_removed', label: '移除成员' },
  { value: 'mcp_invoke', label: 'MCP 调用' },
]

/** 操作日志实体类型字典（target → 可读名） */
export const AUDIT_TARGETS = [
  { value: 'project', label: '项目' },
  { value: 'task', label: '任务' },
  { value: 'role', label: '角色' },
  { value: 'user', label: '用户' },
  { value: 'dept', label: '部门' },
  { value: 'project_member', label: '项目成员' },
  { value: 'project_type', label: '项目类型' },
  { value: 'mcp', label: 'API Key' },
]

/** 数据范围可选值（design-system / data-model 状态字典） */
export const DATA_SCOPES = [
  { value: 'self', label: '本人' },
  { value: 'dept', label: '本部门' },
  { value: 'dept_and_sub', label: '本部门及下属' },
  { value: 'all', label: '全部' },
  { value: 'custom', label: '自定义' },
]

/** 数据范围可配资源（与后端 ScopeResource / SysUserService.RESOURCE 一致；允许自定义扩展） */
export const DATA_SCOPE_RESOURCES = [
  { value: 'user', label: '用户' },
  { value: 'project', label: '项目' },
  { value: 'task', label: '任务' },
]
