import request from './request'

/** 变更中心：变更单台账（只读）。提交入口在各被改业务域，如目标变更走 goalApi.submitChange。 */
export const changeApi = {
  // 台账列表：bizType(goal/...) / bizId / status 可选过滤
  list: (params) => request.get('/changes', { params }),
  get: (id) => request.get(`/changes/${id}`),
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
]
