import request from './request'

/**
 * 简报（人工日/周/月报）。区别于 PMO 度量报表。
 */
export const briefingApi = {
  // 模板列表（内置日/周/月报，惰性生成）
  templates: () => request.get('/briefing-templates'),
  // 我的简报列表（type=daily/weekly/monthly，可空取全部）
  listMine: (type) => request.get('/briefings', { params: type ? { type } : {} }),
  // 简报详情
  get: (id) => request.get(`/briefings/${id}`),
  // 保存草稿（按 模板+周期 幂等）：{ templateId, periodKey, periodStart, periodEnd, content }
  save: (data) => request.post('/briefings', data),
  // 提交
  submit: (id) => request.post(`/briefings/${id}/submit`),
  // 我评审的（type 可空）
  review: (type) => request.get('/briefings/review', { params: type ? { type } : {} }),
  // 成员简报：我评审范围内的成员 id
  reviewees: () => request.get('/briefings/reviewees'),
  // 成员简报：某成员的已提交简报
  members: (type, authorId) =>
    request.get('/briefings/members', { params: { ...(type ? { type } : {}), ...(authorId ? { authorId } : {}) } }),
  // 评审批注列表
  reviews: (id) => request.get(`/briefings/${id}/reviews`),
  // 提交批注：{ comment, action }
  addReview: (id, data) => request.post(`/briefings/${id}/reviews`, data),
  // 跟进问题：我提出的或负责的（status 可空）
  listIssues: (status) => request.get('/briefing-issues', { params: status ? { status } : {} }),
  // 提出问题：{ briefingId, content, ownerId?, dueDate? }
  createIssue: (data) => request.post('/briefing-issues', data),
  // 改问题状态
  updateIssueStatus: (id, status) => request.patch(`/briefing-issues/${id}/status`, { status }),
}
