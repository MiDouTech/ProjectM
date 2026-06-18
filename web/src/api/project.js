import request from './request'

/** 项目 CRUD + 生命周期流转 + 成员（Step 2-1） */
export const projectApi = {
  query: (data) => request.post('/projects/query', data),
  // 我参与的项目（工作台卡）：我负责 ∪ 我是成员
  mine: () => request.get('/projects/mine'),
  get: (id) => request.get(`/projects/${id}`),
  create: (data) => request.post('/projects', data),
  createFromTemplate: (data) => request.post('/projects/from-template', data),
  update: (id, data) => request.put(`/projects/${id}`, data),
  remove: (id) => request.delete(`/projects/${id}`),
  // 手动流转：仅用户态 进行中/结果验收/已结案；注册等系统态由审批驱动
  transition: (id, data) => request.post(`/projects/${id}/transition`, data),
  members: (id) => request.get(`/projects/${id}/members`),
  addMember: (id, data) => request.post(`/projects/${id}/members`, data),
  removeMember: (id, memberId) => request.delete(`/projects/${id}/members/${memberId}`),
  // 提交立项审批：返回审批实例 ID
  submitApproval: (id, form) => request.post(`/projects/${id}/submit-approval`, form),
  // 当前立项审批实例（含待谁审批）：未提交过返回 null
  currentApproval: (id) => request.get(`/projects/${id}/approval`),
  // 活动日志（分页倒序）：params { page, size }
  activities: (id, params) => request.get(`/projects/${id}/activities`, { params }),
}

/** 项目模板（内置 5 套 + 自定义，Step 2-1） */
export const templateApi = {
  list: (category) => request.get('/project-templates', { params: category ? { category } : {} }),
  get: (id) => request.get(`/project-templates/${id}`),
}

/** 通用审批引擎（Step 3） */
export const approvalApi = {
  submit: (data) => request.post('/approvals/submit', data),
  act: (id, data) => request.post(`/approvals/instances/${id}/actions`, data),
  getInstance: (id) => request.get(`/approvals/instances/${id}`),
  // 发起人撤回（仅 pending + 仅申请人）：data { reason? }
  withdraw: (id, data) => request.post(`/approvals/instances/${id}/withdraw`, data),
  // 待我审批（工作台卡）：我未处理且实例 pending 的待办
  mine: () => request.get('/approvals/mine'),
}

/** 审批流定义（Step 3 进度渲染 + P2 可视化设计器 CRUD） */
export const approvalFlowApi = {
  list: (bizType) => request.get('/approval-flows', { params: bizType ? { bizType } : {} }),
  get: (id) => request.get(`/approval-flows/${id}`),
  designerMeta: () => request.get('/approval-flows/designer-meta'),
  create: (data) => request.post('/approval-flows', data),
  update: (id, data) => request.put(`/approval-flows/${id}`, data),
}

/** 立项审批 bizType（与后端 ProjectInitService.BIZ_TYPE 对齐） */
export const APPROVAL_BIZ_TYPES = [
  { value: 'project_init', label: '立项审批' },
  { value: 'cost', label: '费用审批' },
]

/** 项目类型字典（CLAUDE.md §6：S 战略级 / I 创新级 / O 运营级） */
export const PROJECT_CATEGORIES = [
  { value: 'S', label: '战略级', desc: '公司级战略举措' },
  { value: 'I', label: '创新级', desc: '探索性 / POC 创新' },
  { value: 'O', label: '运营级', desc: '常规运营 / 整改 / 督办' },
]

/** O 类细分（CLAUDE.md §6） */
export const O_SUB_CATEGORIES = [
  { value: '常规运营', label: '常规运营' },
  { value: '定向整改', label: '定向整改' },
  { value: '专项督办', label: '专项督办' },
]

/** 用户可手动流转的目标状态（其余系统态由审批/系统驱动，不在此暴露） */
export const MANUAL_TRANSITIONS = [
  { value: '进行中', label: '启动执行（→进行中）', from: ['已注册'] },
  { value: '结果验收', label: '提交结果验收（→结果验收）', from: ['进行中'] },
  { value: '已结案', label: '结案（→已结案）', from: ['结果验收'] },
]
