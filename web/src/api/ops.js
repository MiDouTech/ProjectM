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
  // 自助改密（含首登强制改密）：{oldPassword, newPassword}
  changePassword: (data) => request.post('/platform/auth/password', data),
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
  // 批量状态流转：{ids:[], status, reason} → 处理数量
  batchStatus: (data) => request.post('/platform/tenants/batch-status', data),
  bindSubscription: (id, data) => request.post(`/platform/tenants/${id}/subscription`, data),
  // 用量 vs 配额：[{resource, used, limit, exceeded, snapshotTime}]，limit=-1 表示不限
  usage: (id) => request.get(`/platform/tenants/${id}/usage`),
  // 模拟登录：返回短时租户令牌 {token, tokenType, expiresIn, tenantId, tenantCode, targetUserId}
  impersonate: (id) => request.post(`/platform/tenants/${id}/impersonate`),
  // 数据导出：发起导出，返回 exportId(字符串)
  requestExport: (id) => request.post(`/platform/tenants/${id}/export`),
  // 导出任务列表：[{id,tenantId,status,fileReady,error,createTime,updateTime}]
  exports: (id) => request.get(`/platform/tenants/${id}/exports`),
  // 导出下载：返回 {url}（限时预签名）
  exportDownload: (id, exportId) => request.get(`/platform/tenants/${id}/exports/${exportId}/download`),
  // 发起注销：可选 graceDays，缺省后端默认 30 天
  requestDeletion: (id, graceDays) => request.post(`/platform/tenants/${id}/deletion`, null, { params: graceDays ? { graceDays } : {} }),
  // 取消注销
  cancelDeletion: (id) => request.post(`/platform/tenants/${id}/deletion/cancel`),
}

/** 导出任务状态 */
export const EXPORT_STATUS = [
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'done', label: '已完成' },
  { value: 'failed', label: '失败' },
]

/** 用量监控 */
export const usageApi = {
  snapshot: () => request.post('/platform/usage/snapshot'),
  // 跨租户用量监控：{page,size,onlyExceeded} → PageResult<{tenantId,tenantCode,tenantName,status,usage[],anyExceeded}>
  monitorQuery: (data) => request.post('/platform/usage/tenants/query', data),
}

/** 套餐管理 */
export const planApi = {
  list: () => request.get('/platform/plans'),
  get: (id) => request.get(`/platform/plans/${id}`),
  create: (data) => request.post('/platform/plans', data),
  update: (id, data) => request.put(`/platform/plans/${id}`, data),
  remove: (id) => request.delete(`/platform/plans/${id}`),
  // 套餐功能开关：[{featureCode, enabled}]
  features: (planId) => request.get(`/platform/plans/${planId}/features`),
  saveFeatures: (planId, data) => request.put(`/platform/plans/${planId}/features`, data),
}

/** 收入台账（平台收款 / 退款）*/
export const revenueApi = {
  query: (data) => request.post('/platform/revenue/query', data),
  summary: (tenantId) => request.get('/platform/revenue/summary', { params: { tenantId: tenantId || undefined } }),
  create: (data) => request.post('/platform/revenue', data),
  update: (id, data) => request.put(`/platform/revenue/${id}`, data),
  remove: (id) => request.delete(`/platform/revenue/${id}`),
}

/** 平台公告 */
export const announcementApi = {
  list: () => request.get('/platform/announcements'),
  create: (data) => request.post('/platform/announcements', data),
  update: (id, data) => request.put(`/platform/announcements/${id}`, data),
  remove: (id) => request.delete(`/platform/announcements/${id}`),
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

/** 收入类型 */
export const REVENUE_TYPE = { payment: '收款', refund: '退款' }

/** 收入类型（下拉用数组形态）*/
export const REVENUE_TYPE_OPTIONS = [
  { value: 'payment', label: '收款' },
  { value: 'refund', label: '退款' },
]

/** 公告状态 */
export const ANNOUNCEMENT_STATUS = { draft: '草稿', published: '已发布' }

/** 公告状态（下拉用数组形态）*/
export const ANNOUNCEMENT_STATUS_OPTIONS = [
  { value: 'draft', label: '草稿' },
  { value: 'published', label: '已发布' },
]

/** 公告级别 */
export const ANNOUNCEMENT_LEVEL = { info: '通知', warning: '警告' }

/** 公告级别（下拉用数组形态）*/
export const ANNOUNCEMENT_LEVEL_OPTIONS = [
  { value: 'info', label: '通知' },
  { value: 'warning', label: '警告' },
]

/** 功能码 → 中文名（套餐功能开关 / 租户门控）*/
export const FEATURE_LABELS = {
  gantt: '甘特图',
  okr: '目标/OKR',
  npss: 'NPSS验收',
  doc: '文档',
  cost: '费用',
  report: '报表',
  change: '变更中心',
  openapi: '开放平台',
}
