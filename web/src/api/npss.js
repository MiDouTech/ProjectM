import request from './request'

/** NPSS 价值验收：轮次/评分（幂等）+ 评价方式设置（评价主体）。 */
export const npssApi = {
  listByProject: (projectId) => request.get('/npss/reviews', { params: { projectId } }),
  get: (reviewId) => request.get(`/npss/reviews/${reviewId}`),
  submitScore: (reviewId, data) => request.post(`/npss/reviews/${reviewId}/scores`, data),
  // 租户级评价主体模板（整组保存，replace-all；后端校验合计=100%、受益方≥50%）
  listSubjectTemplates: () => request.get('/npss/subject-templates'),
  saveSubjectTemplates: (items) => request.put('/npss/subject-templates', items),
  // 项目级评价主体（成员即干系人；未配置时返回模板派生草稿）
  listProjectSubjects: (projectId) => request.get(`/npss/projects/${projectId}/subjects`),
  saveProjectSubjects: (projectId, items) => request.put(`/npss/projects/${projectId}/subjects`, items),
}

/** 结果验收（铁三角）：录入/查询 PMO 结论。结案前置硬闸门由后端在项目流转处强制。 */
export const resultVerifyApi = {
  latest: (projectId) => request.get(`/projects/${projectId}/result-verify`),
  // data: { verdict: 'pass'|'fail', onTime, inBudget, inScope, completionRate, remark }
  save: (projectId, data) => request.post(`/projects/${projectId}/result-verify`, data),
}

/** PMO 报表 + 项目/任务度量（只读，数据范围由后端拦截器约束）。 */
export const reportApi = {
  pmoNpss: (year) => request.get('/reports/pmo-npss', { params: year ? { year } : {} }),
  // 组织 NPSS 任意周期 [from, to)（动态计算一定周期内组织得分）
  pmoNpssRange: (from, to) => request.get('/reports/pmo-npss/range', { params: { from, to } }),
  // 报表设置（租户级）：财年起始月
  getSettings: () => request.get('/reports/settings'),
  saveSettings: (data) => request.put('/reports/settings', data),
  overview: () => request.get('/reports/metrics/overview'),
  burndown: (projectId) => request.get('/reports/metrics/burndown', { params: { projectId } }),
  projectHealth: (projectId) => request.get('/reports/metrics/project-health', { params: { projectId } }),
  // 人员负荷：按负责人聚合在办/逾期任务数（数据范围内），负荷降序
  workload: () => request.get('/reports/metrics/workload'),
}

/** result_level → 中文标签（着色由 StatusTag 统一）。 */
export const RESULT_LEVEL_LABEL = { success: '成功', mixed: '混合', failure: '失败' }
