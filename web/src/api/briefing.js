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
}
