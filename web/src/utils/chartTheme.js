/**
 * 图表配色（design-system §7 多视图 / ops §7 克制用色）。
 *
 * G2/canvas 读不了 CSS 变量，这里在运行时把 design token 读成具体值再喂给图表：
 * 既不写裸 hex（值唯一来源仍是 tokens.css），改 token 即全局生效。
 *
 * 克制原则：
 * - 单序列 → 品牌主色（primary）；
 * - 分类对比 → 优先「位置编码 + 直接数值标注」，色板用 token 派生的有限色（categoricalRange），不滥用多彩；
 * - 状态 → 语义色（success/warning/danger/info）；
 * - 趋势多序列 → 配合线型区分（色盲友好），>5 类禁饼。
 */

/** 读取根节点上的 CSS 变量计算值；SSR / 取不到时回落 fallback。 */
export function cssVar(name, fallback = '') {
  if (typeof window === 'undefined' || !document?.documentElement) return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

// token 运行时恒定，记忆化一次即可：图表 option 多在 computed 内调用本helpers，
// 不缓存会每次重算触发多次 getComputedStyle（强制 style recalc）。仅在 token 解析成功后缓存，
// 避免 mount 前拿到空值被永久缓存。
let _colors = null
let _range = null

/** 常用图表色（语义 + 坐标轴/网格），均派生自 token。 */
export function chartColors() {
  if (_colors) return _colors
  const c = {
    primary: cssVar('--el-color-primary'),
    success: cssVar('--el-color-success'),
    warning: cssVar('--el-color-warning'),
    danger: cssVar('--el-color-danger'),
    info: cssVar('--el-color-info'),
    axis: cssVar('--el-text-color-secondary'),
    grid: cssVar('--el-border-color-lighter'),
  }
  if (c.primary) _colors = c
  return c
}

/**
 * 克制分类色板（token 派生，≤5 类）：项目类型 紫/青/绿 + 主色 + 中性。
 * 类别多于色数时 G2 自动循环；分类对比仍以位置 + 数值为主、色为辅。
 */
export function categoricalRange() {
  if (_range) return _range
  const r = [
    cssVar('--mido-cat-s'),
    cssVar('--mido-cat-i'),
    cssVar('--mido-cat-o'),
    cssVar('--el-color-primary'),
    cssVar('--el-color-info'),
  ].filter(Boolean)
  if (r.length) _range = r
  return r
}
