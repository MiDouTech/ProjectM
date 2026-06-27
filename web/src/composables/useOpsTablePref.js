import { reactive } from 'vue'

/**
 * 运营后台表格偏好（列宽记忆 + 列显隐），持久化到 localStorage。
 * 运营态走平台 JWT，不复用租户侧后端表头偏好接口；本地持久化即可。
 *
 * @param {string} listKey 列表唯一标识
 * @param {Array<{key:string,label:string}>} optionalCols 可隐藏列
 */
function safeParse(raw, fallback) {
  try {
    const v = JSON.parse(raw)
    return v == null ? fallback : v
  } catch {
    return fallback
  }
}

export function useOpsTablePref(listKey, optionalCols = []) {
  const wKey = `mido_ops_colw_${listKey}`
  const vKey = `mido_ops_colhidden_${listKey}`
  const widths = reactive(safeParse(localStorage.getItem(wKey), {}))
  const hidden = reactive(new Set(safeParse(localStorage.getItem(vKey), [])))

  // 列拖宽后按列标题记忆（多数列用自定义渲染无 prop，故以 label 为键）
  function onHeaderResize(newWidth, oldWidth, column) {
    const k = column && column.label
    if (!k) return
    widths[k] = Math.round(newWidth)
    localStorage.setItem(wKey, JSON.stringify(widths))
  }
  function w(label, fallback) {
    return widths[label] || fallback
  }
  function visible(key) {
    return !hidden.has(key)
  }
  function toggle(key, show) {
    if (show) hidden.delete(key)
    else hidden.add(key)
    localStorage.setItem(vKey, JSON.stringify([...hidden]))
  }

  return { onHeaderResize, w, visible, toggle, optionalCols }
}
