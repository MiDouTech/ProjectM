<template>
  <div class="mido-gantt" v-loading="loading">
    <div class="mido-gantt__bar">
      <div class="mido-gantt__legend mido-text-secondary">
        <span><i class="sw sw--bar" />任务</span>
        <span><i class="sw sw--critical" />关键路径</span>
        <span><i class="sw sw--milestone" />里程碑</span>
        <span><i class="sw sw--baseline" />基线</span>
      </div>
      <div class="mido-gantt__actions">
        <el-button link type="primary" @click="setBaseline">设为基线</el-button>
        <el-button v-if="baseline.length" link @click="clearBaseline">清除基线</el-button>
      </div>
    </div>

    <div v-show="hasBars" ref="ganttEl" class="mido-gantt__canvas"></div>
    <el-empty v-show="!hasBars && !loading" description="暂无可排期任务（需任务含开始/截止日期）" :image-size="60" />
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Gantt from 'frappe-gantt'
import 'frappe-gantt/dist/frappe-gantt.css'
import { taskApi, dependencyApi } from '@/api/task'

const props = defineProps({
  projectId: { type: [Number, String], default: null },
})

const loading = ref(false)
const hasBars = ref(false)
const ganttEl = ref(null)
const gantt = shallowRef(null)
const rawTasks = ref([])              // 原始 TaskVO（改期回传用）
const baseline = ref([])              // 基线快照 [{id,start,end}]（客户端，不持久化）
let curById = {}                      // id -> {start,end} 当前排期（基线/里程碑装饰用）

const SVG_NS = 'http://www.w3.org/2000/svg'

function fmtDate(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
function daysBetween(a, b) {
  return (Date.parse(b) - Date.parse(a)) / 86400000
}

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    const [page, deps, cp] = await Promise.all([
      taskApi.query({ page: 1, size: 500, projectId: props.projectId }),
      dependencyApi.listByProject(props.projectId),
      dependencyApi.criticalPath(props.projectId),
    ])
    rawTasks.value = page.list || []
    const critical = new Set((cp.criticalTaskIds || []).map(String))
    // 后继 -> 其前置 id 列表（frappe dependencies 指向"我依赖谁"）
    const predsOf = {}
    deps.forEach((d) => {
      (predsOf[d.successorId] ||= []).push(String(d.predecessorId))
    })
    buildAndRender(critical, predsOf)
  } finally {
    loading.value = false
  }
}

function buildAndRender(critical, predsOf) {
  curById = {}
  const frappeTasks = rawTasks.value
    .filter((t) => t.startDate && (t.dueDate || t.isMilestone === 1))
    .map((t) => {
      const milestone = t.isMilestone === 1
      const start = t.startDate
      const end = milestone ? t.startDate : t.dueDate
      curById[String(t.id)] = { start, end }
      const cls = [
        critical.has(String(t.id)) ? 'gantt-critical' : '',
        milestone ? 'gantt-milestone' : '',
      ].filter(Boolean).join(' ')
      return {
        id: String(t.id),
        name: t.title,
        start,
        end,
        dependencies: (predsOf[t.id] || []).join(','),
        custom_class: cls,
      }
    })

  hasBars.value = frappeTasks.length > 0
  if (gantt.value && ganttEl.value) {
    ganttEl.value.innerHTML = '' // 重建前清理，避免堆叠
    gantt.value = null
  }
  if (!hasBars.value) return

  nextTick(() => {
    gantt.value = new Gantt(ganttEl.value, frappeTasks, {
      view_mode: 'Day',
      view_mode_select: true,
      today_button: true,
      infinite_padding: false,
      readonly_progress: true,
      on_date_change: handleDateChange,
      on_view_change: () => nextTick(decorate),
    })
    nextTick(decorate)
  })
}

async function handleDateChange(task, start, end) {
  const orig = rawTasks.value.find((t) => String(t.id) === String(task.id))
  if (!orig) return
  try {
    await taskApi.update(orig.id, {
      title: orig.title,
      priority: orig.priority,
      stage: orig.stage,
      startDate: fmtDate(start),
      dueDate: fmtDate(end),
      isMilestone: orig.isMilestone,
      description: orig.description,
    })
    ElMessage.success('已更新排期')
    load() // 重新计算关键路径并重绘
  } catch {
    ElMessage.error('排期更新失败，已还原')
    load()
  }
}

// 里程碑方块化为菱形 + 基线叠加条（纯 DOM/SVG 装饰，frappe 无原生支持）
function decorate() {
  const el = ganttEl.value
  if (!el) return
  // 里程碑：把短条改成正方形（CSS 旋转 45° 成菱形）
  el.querySelectorAll('.bar-wrapper.gantt-milestone .bar').forEach((bar) => {
    const h = +bar.getAttribute('height')
    const w = +bar.getAttribute('width')
    const x = +bar.getAttribute('x')
    if (w !== h) {
      bar.setAttribute('width', h)
      bar.setAttribute('x', x + w / 2 - h / 2)
    }
  })
  // 基线：按各任务条自身几何换算像素/天，叠加浅色基线条
  el.querySelectorAll('.gantt-baseline-rect').forEach((n) => n.remove())
  if (!baseline.value.length) return
  const blById = Object.fromEntries(baseline.value.map((b) => [String(b.id), b]))
  el.querySelectorAll('.bar-wrapper').forEach((wrapper) => {
    const id = wrapper.getAttribute('data-id')
    const bl = blById[id]
    const cur = curById[id]
    if (!bl || !cur) return
    const curDays = daysBetween(cur.start, cur.end)
    if (curDays <= 0) return // 里程碑/零工期跳过
    const bar = wrapper.querySelector('.bar')
    if (!bar) return
    const x = +bar.getAttribute('x')
    const y = +bar.getAttribute('y')
    const w = +bar.getAttribute('width')
    const h = +bar.getAttribute('height')
    const pxPerDay = w / curDays
    const rect = document.createElementNS(SVG_NS, 'rect')
    rect.setAttribute('class', 'gantt-baseline-rect')
    rect.setAttribute('x', x + daysBetween(cur.start, bl.start) * pxPerDay)
    rect.setAttribute('y', y + h + 2)
    rect.setAttribute('width', Math.max(daysBetween(bl.start, bl.end), 0.5) * pxPerDay)
    rect.setAttribute('height', 3)
    rect.setAttribute('rx', 1.5)
    wrapper.appendChild(rect)
  })
}

function setBaseline() {
  baseline.value = Object.entries(curById).map(([id, v]) => ({ id, start: v.start, end: v.end }))
  ElMessage.success('已设为基线')
  nextTick(decorate)
}
function clearBaseline() {
  baseline.value = []
  nextTick(decorate)
}

onBeforeUnmount(() => {
  if (ganttEl.value) ganttEl.value.innerHTML = ''
  gantt.value = null
})

watch(() => props.projectId, load, { immediate: true })
</script>

<!-- 非 scoped：frappe 在普通 div 内生成 DOM，需用命名空间 .mido-gantt 限定，避免泄漏 -->
<style>
.mido-gantt__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.mido-gantt__legend {
  display: flex;
  gap: var(--mido-space-4);
  font-size: var(--mido-font-size-caption);
}
.mido-gantt__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
}
.mido-gantt .sw {
  width: 12px;
  height: 12px;
  border-radius: var(--mido-radius-sm);
  display: inline-block;
}
.mido-gantt .sw--bar { background: var(--el-color-primary-light-3); }
.mido-gantt .sw--critical { background: var(--el-color-danger); }
.mido-gantt .sw--milestone { background: var(--el-color-warning); transform: rotate(45deg); }
.mido-gantt .sw--baseline { background: var(--el-text-color-placeholder); height: 4px; }

/* frappe --g-* 主题变量 → design-system token（不裸色值） */
.mido-gantt .gantt-container {
  --g-bar-color: var(--el-color-primary-light-5);
  --g-bar-border: var(--el-border-color);
  --g-progress-color: var(--el-color-primary);
  --g-arrow-color: var(--el-text-color-secondary);
  --g-handle-color: var(--el-color-primary-dark-2);
  --g-today-highlight: var(--el-color-danger);
  --g-border-color: var(--el-border-color-light);
  --g-row-border-color: var(--el-border-color);
  --g-tick-color: var(--el-border-color-light);
  --g-tick-color-thick: var(--el-border-color);
  --g-text-dark: var(--el-text-color-primary);
  --g-text-light: var(--el-bg-color);
  --g-text-muted: var(--el-text-color-secondary);
  --g-header-background: var(--el-bg-color);
  --g-row-color: var(--el-bg-color);
  --g-actions-background: var(--el-fill-color-light);
  --g-weekend-highlight-color: var(--el-fill-color-light);
}

/* 关键路径高亮 */
.mido-gantt .bar-wrapper.gantt-critical .bar-progress { fill: var(--el-color-danger); }
.mido-gantt .bar-wrapper.gantt-critical .bar { outline-color: var(--el-color-danger); }
/* 里程碑：方块旋转 45° 成菱形 */
.mido-gantt .bar-wrapper.gantt-milestone .bar {
  fill: var(--el-color-warning);
  transform-box: fill-box;
  transform-origin: center;
  transform: rotate(45deg);
}
/* 基线叠加条 */
.mido-gantt .gantt-baseline-rect { fill: var(--el-text-color-placeholder); }
</style>
