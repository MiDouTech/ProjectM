/**
 * 页面配置（ADR-0004 · L3）前端共用工具。
 */

/** 自定义字段 options 字符串安全解析为数组（非法 JSON 回落空数组）。 */
export function parseFieldOptions(json) {
  try {
    return json ? JSON.parse(json) : []
  } catch {
    return []
  }
}
