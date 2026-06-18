import request from './request'

/** 目标/KR + 对齐网。progress 为后端计算值，前端只读展示。 */
export const goalApi = {
  list: (params) => request.get('/goals', { params }),
  get: (id) => request.get(`/goals/${id}`),
  create: (data) => request.post('/goals', data),
  update: (id, data) => request.put(`/goals/${id}`, data),
  // 量化指标行内编辑：仅当前值，进度后端自动重算
  updateMetric: (id, metricCurrent) => request.put(`/goals/${id}/metric`, { metricCurrent }),
  remove: (id) => request.delete(`/goals/${id}`),
  alignGraph: () => request.get('/goals/align-graph'),
  listAlignments: (id) => request.get(`/goals/${id}/alignments`),
  addAlignment: (id, data) => request.post(`/goals/${id}/alignments`, data),
  removeAlignment: (alignmentId) => request.delete(`/goals/alignments/${alignmentId}`),
  // 反向查询：对齐到某对象的目标（项目工作台·目标用）
  listByTarget: (targetType, targetId) =>
    request.get('/goals/by-target', { params: { targetType, targetId } }),
  byProject: (projectId) => goalApi.listByTarget('project', projectId),
}

export const GOAL_TYPES = [
  { value: 'objective', label: '目标' },
  { value: 'kr', label: 'KR' },
]
export const ALIGN_TARGET_TYPES = [
  { value: 'project', label: '项目' },
  { value: 'task', label: '任务' },
]
