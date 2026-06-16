/**
 * FilterBuilder 输出（{ match, rules }）对一行记录求值。
 * 纯前端筛选，作用于已加载结果集（服务端粗筛 + 前端精筛分工，见 ProjectListView 注释）。
 */
function testRule(row, rule) {
  const raw = row[rule.field]
  const op = rule.op
  if (op === 'contains') {
    return String(raw ?? '').toLowerCase().includes(String(rule.value).toLowerCase())
  }
  // 数值比较：两侧可转数字时按数字比，否则按字符串
  const a = Number(raw)
  const b = Number(rule.value)
  const numeric = !Number.isNaN(a) && !Number.isNaN(b)
  const l = numeric ? a : String(raw ?? '')
  const r = numeric ? b : String(rule.value)
  switch (op) {
    case 'eq': return l === r
    case 'neq': return l !== r
    case 'gt': return l > r
    case 'gte': return l >= r
    case 'lt': return l < r
    case 'lte': return l <= r
    default: return true
  }
}

export function applyFilter(rows, filter) {
  if (!filter || !filter.rules || !filter.rules.length) return rows
  const { match, rules } = filter
  return rows.filter((row) =>
    match === 'or'
      ? rules.some((rule) => testRule(row, rule))
      : rules.every((rule) => testRule(row, rule)),
  )
}

/** 排序：field + asc/desc，数值/日期/字符串自适应。 */
export function applySort(rows, field, order) {
  if (!field) return rows
  const dir = order === 'desc' ? -1 : 1
  return [...rows].sort((x, y) => {
    const a = x[field]
    const b = y[field]
    if (a == null && b == null) return 0
    if (a == null) return 1
    if (b == null) return -1
    const na = Number(a)
    const nb = Number(b)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return (na - nb) * dir
    return String(a).localeCompare(String(b)) * dir
  })
}
