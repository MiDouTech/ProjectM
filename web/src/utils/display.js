/**
 * 展示层共享工具：本地日期、任务逾期判定、用户名解析、时间格式化。
 * 抽出以消除 TaskWorkspaceView / WorkbenchCard / TaskDetailDrawer / CommentThread 的重复实现。
 */
import { TASK_TERMINAL } from '@/api/task'

/** 本地时区当天 YYYY-MM-DD（不要用 toISOString，那是 UTC，UTC+8 凌晨会差一天）。 */
export function todayStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 任务是否逾期：有截止、已过期、且未到终态。 */
export function isTaskOverdue(task, today = todayStr()) {
  return !!task?.dueDate && task.dueDate < today && !TASK_TERMINAL.includes(task.status)
}

/** 用户 ID → 姓名；未命中回落「用户#id」，空值回落「—」。
 *  按字符串比较：选人组件回传雪花 ID 为字符串，后端为数字，避免严格相等漏匹配。 */
export function userName(users, id) {
  if (id == null || id === '') return '—'
  return users?.find((u) => String(u.id) === String(id))?.name || `用户#${id}`
}

/** LocalDateTime 字符串截断展示（默认到分钟）。 */
export function formatDateTime(t, start = 0, end = 16) {
  return t ? String(t).replace('T', ' ').slice(start, end) : ''
}

/**
 * 通知 → 跳转目标：优先用后端给的 link；否则按 bizType/bizId 回落。无法定位返回 null。
 * 供工作台通知卡与通知列表页共用。
 */
export function notificationRoute(n) {
  if (n?.link) return n.link
  if (!n?.bizId) return null
  if (n.bizType === 'approval') return { path: '/approval', query: { open: n.bizId } }
  if (n.bizType === 'project') return `/project/${n.bizId}`
  return null
}

/** 解析通知正文（payload JSON 里的 content）。 */
export function notificationContent(n) {
  if (!n?.payload) return ''
  try {
    return JSON.parse(n.payload).content || ''
  } catch {
    return ''
  }
}
