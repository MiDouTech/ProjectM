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
}

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
