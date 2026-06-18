import request from './request'

/** 费用：CRUD + 提报审批（biz_type=cost）。 */
export const costApi = {
  listByProject: (projectId) => request.get('/costs', { params: { projectId } }),
  get: (id) => request.get(`/costs/${id}`),
  create: (data) => request.post('/costs', data),
  update: (id, data) => request.put(`/costs/${id}`, data),
  remove: (id) => request.delete(`/costs/${id}`),
  submit: (id) => request.post(`/costs/${id}/submit`),
}

/** 费用科目字典（住宿/餐费/差旅/制作/服务费...，允许自定义） */
export const COST_ACCOUNTS = ['住宿', '餐费', '差旅', '制作', '服务费', '其他']

/** 费用状态（pm_cost.status） */
export const COST_STATUSES = ['未发生', '已发生', '被退回']
