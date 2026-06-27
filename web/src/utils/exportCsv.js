/**
 * 将数据导出为 CSV 文件（前端生成，含 UTF-8 BOM 以兼容 Excel 中文）。
 *
 * @param {string} filename 文件名（不含扩展名）
 * @param {Array<{key:string,title:string}>} columns 列定义
 * @param {Array<Object>} rows 数据行
 */
export function exportCsv(filename, columns, rows) {
  const escape = (v) => {
    const s = v == null ? '' : String(v)
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  const header = columns.map((c) => escape(c.title)).join(',')
  const body = (rows || [])
    .map((r) => columns.map((c) => escape(r[c.key])).join(','))
    .join('\n')
  const csv = '﻿' + header + '\n' + body
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${filename}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
