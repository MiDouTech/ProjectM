import request from './request'

/** 变更中心：变更单台账（只读）。提交入口在各被改业务域，如目标变更走 goalApi.submitChange。 */
export const changeApi = {
  // 台账列表：bizType(goal/...) / bizId / status 可选过滤
  list: (params) => request.get('/changes', { params }),
  get: (id) => request.get(`/changes/${id}`),
}

/** 变更策略管理（租户自配各变更类型 必审/免审 + 绑定审批流） */
export const changePolicyApi = {
  list: () => request.get('/change-policies'),
  // 按 changeType 幂等保存：{ changeType, requireApproval(0/1), flowId, enabled(0/1) }
  save: (data) => request.put('/change-policies', data),
}

// 状态码→中文（着色统一交 StatusTag，按中文标签映射，页面禁写 tag type）
export const CHANGE_STATUS = [
  { value: 'pending', label: '审批中' },
  { value: 'applied', label: '已生效' },
  { value: 'rejected', label: '已驳回' },
  { value: 'withdrawn', label: '已撤回' },
]
export const CHANGE_TYPES = [
  { value: 'goal_target', label: '目标值调整' },
  { value: 'goal_scope', label: '范围/口径变更' },
  { value: 'goal_owner', label: '负责人变更' },
  { value: 'goal_period', label: '周期变更' },
  // goal_close(目标关闭)暂未开放：pm_goal 无状态列，待目标状态机落地再加
  { value: 'project_schedule', label: '项目时间变更' },
  { value: 'task_baseline', label: '重大任务变更' },
]

// 变更对象域 biz_type → 中文（变更台账展示）
export const CHANGE_BIZ_LABEL = { goal: '目标', project: '项目', task: '任务' }

// 变更字段 key → 中文（变更台账 before→after diff 展示，跨域统一登记）
export const CHANGE_FIELD_LABEL = {
  title: '标题', ownerId: '负责人', assigneeId: '负责人', period: '周期',
  metricUnit: '单位', metricStart: '指标起点', metricTarget: '指标目标',
  startDate: '开始日期', endDate: '结束日期', dueDate: '截止日期',
}
