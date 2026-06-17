import request from './request'

/** NPSS 价值验收：轮次/评分（幂等）。 */
export const npssApi = {
  listByProject: (projectId) => request.get('/npss/reviews', { params: { projectId } }),
  get: (reviewId) => request.get(`/npss/reviews/${reviewId}`),
  submitScore: (reviewId, data) => request.post(`/npss/reviews/${reviewId}/scores`, data),
}

/** PMO 报表 + 项目/任务度量（只读，数据范围由后端拦截器约束）。 */
export const reportApi = {
  pmoNpss: (year) => request.get('/reports/pmo-npss', { params: year ? { year } : {} }),
  overview: () => request.get('/reports/metrics/overview'),
  burndown: (projectId) => request.get('/reports/metrics/burndown', { params: { projectId } }),
  projectHealth: (projectId) => request.get('/reports/metrics/project-health', { params: { projectId } }),
}

/** result_level → 中文标签（着色由 StatusTag 统一）。 */
export const RESULT_LEVEL_LABEL = { success: '成功', mixed: '混合', failure: '失败' }
