import request from './request'

/** NPSS 价值验收：轮次/评分（幂等）。 */
export const npssApi = {
  listByProject: (projectId) => request.get('/npss/reviews', { params: { projectId } }),
  get: (reviewId) => request.get(`/npss/reviews/${reviewId}`),
  submitScore: (reviewId, data) => request.post(`/npss/reviews/${reviewId}/scores`, data),
}

/** PMO 报表：总体评价（成功%−失败%，对比基线 36）。 */
export const reportApi = {
  pmoNpss: (year) => request.get('/reports/pmo-npss', { params: year ? { year } : {} }),
}

/** result_level → 中文标签（着色由 StatusTag 统一）。 */
export const RESULT_LEVEL_LABEL = { success: '成功', mixed: '混合', failure: '失败' }
