import { computed, ref } from 'vue'

/**
 * 客户端分页 + 排序（用于全量返回、数据量小的列表：套餐/公告/运营账号）。
 * 后端无分页接口时在前端切片，避免列表随数据增长无上限。
 *
 * @param {import('vue').Ref<Array>} rowsRef 全量数据 ref
 * @param {number} pageSize 每页条数
 */
export function useClientTable(rowsRef, pageSize = 20) {
  const page = ref(1)
  const size = ref(pageSize)
  const sortProp = ref('')
  const sortOrder = ref('') // 'ascending' | 'descending' | ''

  const sorted = computed(() => {
    const list = [...(rowsRef.value || [])]
    if (!sortProp.value || !sortOrder.value) return list
    const dir = sortOrder.value === 'ascending' ? 1 : -1
    return list.sort((a, b) => {
      const va = a[sortProp.value]
      const vb = b[sortProp.value]
      if (va == null) return 1
      if (vb == null) return -1
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb), 'zh-CN') * dir
    })
  })

  const total = computed(() => sorted.value.length)
  const paged = computed(() => {
    const from = (page.value - 1) * size.value
    return sorted.value.slice(from, from + size.value)
  })

  function onSort({ prop, order }) {
    sortProp.value = prop
    sortOrder.value = order
    page.value = 1
  }
  function reset() {
    page.value = 1
  }

  return { page, size, total, paged, onSort, reset }
}
