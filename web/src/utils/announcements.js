/**
 * 平台公告「已看」态（前端本地维护）。
 *
 * 背景：平台公告（sys_announcement）是跨租户全局表，无 per-user 已读态；为把公告并入
 * 消息中心又不污染租户表/不破坏多租户隔离，已看状态按「用户 + 公告 id」记在 localStorage。
 * 顶栏徽标与消息中心页共用本工具，保证未读计数一致。
 */

const KEY_PREFIX = 'mido_anno_seen_'

function keyOf(userId) {
  return `${KEY_PREFIX}${userId || 'anon'}`
}

/** 读取某用户已看公告 id 集合（容错：解析失败返回空集）。 */
export function seenIds(userId) {
  try {
    const raw = localStorage.getItem(keyOf(userId))
    return new Set(raw ? JSON.parse(raw) : [])
  } catch {
    return new Set()
  }
}

/** 标记某公告为已看。 */
export function markSeen(userId, id) {
  if (id == null) return
  const set = seenIds(userId)
  set.add(id)
  try {
    localStorage.setItem(keyOf(userId), JSON.stringify([...set]))
  } catch {
    /* localStorage 不可用时忽略，仅影响本地未读标记 */
  }
}

/** 未看公告数量。 */
export function unseenCount(announcements, userId) {
  const set = seenIds(userId)
  return (announcements || []).filter((a) => !set.has(a.id)).length
}

/** 某公告是否已看。 */
export function isSeen(announcement, userId) {
  return seenIds(userId).has(announcement?.id)
}
