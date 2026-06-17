/**
 * 展示层共享工具：本地日期、任务逾期判定、用户名解析、时间格式化。
 * 抽出以消除 TaskWorkspaceView / WorkbenchCard / TaskDetailDrawer / CommentThread 的重复实现。
 */

/** 本地时区当天 YYYY-MM-DD（不要用 toISOString，那是 UTC，UTC+8 凌晨会差一天）。 */
export function todayStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

// 任务终态：达到后不再计逾期（与后端 overdue 口径一致）
const TASK_TERMINAL = ['已完成', '已验收']

/** 任务是否逾期：有截止、已过期、且未到终态。 */
export function isTaskOverdue(task, today = todayStr()) {
  return !!task?.dueDate && task.dueDate < today && !TASK_TERMINAL.includes(task.status)
}

/** 用户 ID → 姓名；未命中回落「用户#id」，空值回落「—」。 */
export function userName(users, id) {
  return users?.find((u) => u.id === id)?.name || (id ? `用户#${id}` : '—')
}

/** LocalDateTime 字符串截断展示（默认到分钟）。 */
export function formatDateTime(t, start = 0, end = 16) {
  return t ? String(t).replace('T', ' ').slice(start, end) : ''
}
