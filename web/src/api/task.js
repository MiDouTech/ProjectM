import request from './request'

/** 任务 CRUD / 子任务 / 指派 / 状态流转(看板拖拽) / 列表 / 看板（Step 4） */
export const taskApi = {
  query: (data) => request.post('/tasks/query', data),
  // 按视图查询（viewId 或内联 config）→ 分组/排序/筛选后的任务
  viewQuery: (data) => request.post('/tasks/view-query', data),
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
  // 发起重大任务变更（受控，走变更中心+审批引擎）/ 本任务变更历史
  submitChange: (id, data) => request.post(`/tasks/${id}/changes`, data),
  changes: (id) => request.get(`/tasks/${id}/changes`),
  // 批量操作：每条后端各自发领域事件；批量改状态逐条校验工作流，任一非法整批回滚
  batchTransition: (ids, targetStatus) => request.post('/tasks/batch/transition', { ids, targetStatus }),
  batchAssign: (ids, assigneeId) => request.post('/tasks/batch/assignee', { ids, assigneeId }),
  batchDelete: (ids) => request.post('/tasks/batch/delete', { ids }),
}

/** 工时：登记/修改 + 记录列表 + 任务级汇总（项目级/人员级供统计页，工时 Tab 用任务级） */
export const workHourApi = {
  list: (taskId) => request.get('/work-hours', { params: { taskId } }),
  log: (data) => request.post('/work-hours', data),
  update: (id, data) => request.put(`/work-hours/${id}`, data),
  taskSummary: (taskId) => request.get('/work-hours/summary/task', { params: { taskId } }),
}

/** 工时类型 / 类别字典（与后端校验集合一致） */
export const WORKHOUR_KINDS = [
  { value: 'est', label: '预估' },
  { value: 'actual', label: '实际' },
]
export const WORKHOUR_CATEGORIES = ['设计', '研发', '文档', '测试', '其他']

/** 任务依赖：增删 + 项目依赖清单 + 关键路径（基于依赖+工期） */
export const dependencyApi = {
  listByProject: (projectId) => request.get('/task-dependencies', { params: { projectId } }),
  criticalPath: (projectId) => request.get('/task-dependencies/critical-path', { params: { projectId } }),
  add: (data) => request.post('/task-dependencies', data),
  remove: (id) => request.delete(`/task-dependencies/${id}`),
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

/** 工作项类型 + 工作流（阶段2）。流转矩阵驱动状态流转引擎；字段绑定供表单布局。 */
export const workItemTypeApi = {
  list: (onlyActive = false) => request.get('/work-item-types', { params: { onlyActive } }),
  create: (data) => request.post('/work-item-types', data),
  update: (id, data) => request.put(`/work-item-types/${id}`, data),
  remove: (id) => request.delete(`/work-item-types/${id}`),
  getFields: (id) => request.get(`/work-item-types/${id}/fields`),
  saveFields: (id, fields) => request.put(`/work-item-types/${id}/fields`, fields),
  getTransitions: (id) => request.get(`/work-item-types/${id}/transitions`),
  saveTransitions: (id, transitions) => request.put(`/work-item-types/${id}/transitions`, transitions),
}

/** 工作项类型可绑定的系统字段键（与后端 TaskService 字段键一致；自定义字段另由 field-defs 提供） */
export const SYSTEM_TASK_FIELDS = [
  { value: 'title', label: '标题' },
  { value: 'status', label: '状态' },
  { value: 'priority', label: '优先级' },
  { value: 'assignee', label: '负责人' },
  { value: 'stage', label: '阶段' },
  { value: 'startDate', label: '开始日期' },
  { value: 'dueDate', label: '截止日期' },
  { value: 'isMilestone', label: '里程碑' },
  { value: 'description', label: '描述' },
]

/** 状态库（租户自配任务状态字典，阶段1-b；双轨，暂未接管 task 流转） */
export const statusApi = {
  list: (onlyActive = false) => request.get('/statuses', { params: { onlyActive } }),
  create: (data) => request.post('/statuses', data),
  update: (id, data) => request.put(`/statuses/${id}`, data),
  remove: (id) => request.delete(`/statuses/${id}`),
}

/** 状态元类别（与后端 MetaCategory 一致；统计口径基准） */
export const META_CATEGORIES = [
  { value: '未开始', label: '未开始' },
  { value: '进行中', label: '进行中' },
  { value: '已完成', label: '已完成' },
]

/** 优先级模式（租户自配，阶段1-c；双轨，暂未接管 task 优先级） */
export const priorityModeApi = {
  list: () => request.get('/priority-modes'),
  get: (id) => request.get(`/priority-modes/${id}`),
  create: (data) => request.post('/priority-modes', data),
  update: (id, data) => request.put(`/priority-modes/${id}`, data),
  remove: (id) => request.delete(`/priority-modes/${id}`),
}
