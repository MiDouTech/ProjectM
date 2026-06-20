import request from './request'

/**
 * 平台运营后台 API（接口前缀 /platform/**，baseURL=/api/v1）。
 * 复用唯一 Axios 实例；token 由 request.js 按 /ops 路径前缀选择注入。
 * 雪花 ID 一律按字符串处理，禁止 Number() 转换。
 */

/** 运营认证 */
export const opsAuthApi = {
  login: (data) => request.post('/platform/auth/login', data),
  me: () => request.get('/platform/auth/me'),
}

/** 仪表盘 */
export const dashboardApi = {
  overview: () => request.get('/platform/dashboard/overview'),
}

/** 租户管理 */
export const tenantApi = {
  query: (data) => request.post('/platform/tenants/query', data),
  get: (id) => request.get(`/platform/tenants/${id}`),
  create: (data) => request.post('/platform/tenants', data),
  update: (id, data) => request.put(`/platform/tenants/${id}`, data),
  changeStatus: (id, data) => request.put(`/platform/tenants/${id}/status`, data),
  bindSubscription: (id, data) => request.post(`/platform/tenants/${id}/subscription`, data),
  // 用量 vs 配额：[{resource, used, limit, exceeded, snapshotTime}]，limit=-1 表示不限
  usage: (id) => request.get(`/platform/tenants/${id}/usage`),
  // 模拟登录：返回短时租户令牌 {token, tokenType, expiresIn, tenantId, tenantCode, targetUserId}
  impersonate: (id) => request.post(`/platform/tenants/${id}/impersonate`),
}

/** 用量快照（手动触发全量，返回处理租户数）*/
export const usageApi = {
  snapshot: () => request.post('/platform/usage/snapshot'),
}

/** 套餐管理 */
export const planApi = {
  list: () => request.get('/platform/plans'),
  get: (id) => request.get(`/platform/plans/${id}`),
  create: (data) => request.post('/platform/plans', data),
  update: (id, data) => request.put(`/platform/plans/${id}`, data),
  remove: (id) => request.delete(`/platform/plans/${id}`),
}

/** 运营账号管理 */
export const platformAdminApi = {
  roles: () => request.get('/platform/roles'),
  list: () => request.get('/platform/admins'),
  create: (data) => request.post('/platform/admins', data),
  update: (id, data) => request.put(`/platform/admins/${id}`, data),
  resetPassword: (id, password) => request.put(`/platform/admins/${id}/password`, { password }),
}

/** 审计日志 */
export const auditApi = {
  query: (data) => request.post('/platform/audit/query', data),
}

/** ===== 状态字典常量（与后端英文码 + StatusTag 映射一致）===== */

/** 租户生命周期状态 */
export const TENANT_STATUS = [
  { value: 'trial', label: '试用' },
  { value: 'active', label: '正式' },
  { value: 'suspended', label: '停用' },
  { value: 'expired', label: '已过期' },
  { value: 'closed', label: '已注销' },
]

/** 租户状态流转可选目标（status 接口仅接受 active|suspended|closed）*/
export const TENANT_STATUS_ACTIONS = [
  { value: 'active', label: '启用' },
  { value: 'suspended', label: '停用' },
  { value: 'closed', label: '注销' },
]

/** 套餐 / 运营账号通用启停状态 */
export const ENABLE_STATUS = [
  { value: 'active', label: '启用' },
  { value: 'disabled', label: '停用' },
]

/** 套餐计费周期 */
export const BILLING_CYCLE = [
  { value: 'monthly', label: '按月' },
  { value: 'yearly', label: '按年' },
  { value: 'once', label: '一次性' },
]

/** 配额资源项（-1=不限）*/
export const QUOTA_RESOURCE = [
  { value: 'user', label: '成员数' },
  { value: 'project', label: '项目数' },
  { value: 'storage_mb', label: '存储(MB)' },
  { value: 'task', label: '任务数' },
]
