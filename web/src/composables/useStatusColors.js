import { ref } from 'vue'
import { statusApi } from '@/api/task'

/**
 * 共享状态色映射（阶段·翻转读方第3步）：一次加载状态库 name→color，全局复用。
 * 任意展示任务状态处用 statusColor(name) 取设计 token 传给 StatusTag，使自定义状态按状态库着色。
 * 未配置状态库 / 未命中时返回 ''，StatusTag 回落内置名称映射（行为不变）。
 */
const colorMap = ref({})
let loaded = false
let loading = null

function ensureLoaded() {
  if (loaded || loading) return
  loading = statusApi
    .list(true)
    .then((list) => {
      const m = {}
      for (const s of list || []) m[s.name] = s.color
      colorMap.value = m
      loaded = true
    })
    .catch(() => {})
    .finally(() => {
      loading = null
    })
}

export function useStatusColors() {
  ensureLoaded()
  const statusColor = (name) => colorMap.value[name] || ''
  return { statusColor }
}
