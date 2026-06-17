import request from './request'

/** 任务 CRUD / 子任务 / 指派 / 状态流转(看板拖拽) / 列表 / 看板（Step 4） */
export const taskApi = {
  query: (data) => request.post('/tasks/query', data),
  get: (id) => request.get(`/tasks/${id}`),
  subtasks: (id) => request.get(`/tasks/${id}/subtasks`),
  create: (data) => request.post('/tasks', data),
  update: (id, data) => request.put(`/tasks/${id}`, data),
  remove: (id) => request.delete(`/tasks/${id}`),
  assign: (id, assigneeId) => request.put(`/tasks/${id}/assignee`, { assigneeId }),
  // 看板拖拽改状态亦走此（后端校验工作流合法流转，非法返回 409）
  transition: (id, targetStatus) => request.post(`/tasks/${id}/transition`, { targetStatus }),
  kanban: (projectId) => request.get('/tasks/kanban', { params: { projectId } }),
  // 活动日志（分页倒序）：params { page, size }
  activities: (id, params) => request.get(`/tasks/${id}/activities`, { params }),
  // 批量操作：每条后端各自发领域事件；批量改状态逐条校验工作流，任一非法整批回滚
  batchTransition: (ids, targetStatus) => request.post('/tasks/batch/transition', { ids, targetStatus }),
  batchAssign: (ids, assigneeId) => request.post('/tasks/batch/assignee', { ids, assigneeId }),
  batchDelete: (ids) => request.post('/tasks/batch/delete', { ids }),
}

/** 任务默认工作流状态（data-model 状态字典；看板列序） */
export const TASK_STATUSES = ['未开始', '进行中', '已完成', '已验收']

/** 任务终态：达到后不再计逾期（与后端 overdue 口径一致） */
export const TASK_TERMINAL = ['已完成', '已验收']

/** 默认工作流合法流转表（前端用于拖拽前的可达性预判，最终以后端校验为准） */
export const TASK_TRANSITIONS = {
  未开始: ['进行中'],
  进行中: ['未开始', '已完成'],
  已完成: ['进行中', '已验收'],
  已验收: ['已完成'],
}

/** 优先级字典（1 高 / 2 中 / 3 低） */
export const TASK_PRIORITIES = [
  { value: 1, label: '高' },
  { value: 2, label: '中' },
  { value: 3, label: '低' },
]
