<template>
  <div class="mido-page">
    <!-- 品牌温度页头（design-system §1.6）：浅色淡底克制点睛，弱化视觉权重、不抢注意力 -->
    <div class="wb__hero">
      <div class="wb__greeting">
        <span class="wb__hello">{{ greeting }}{{ myName ? '，' + myName : '' }}</span>
        <span class="wb__date">{{ todayText }}</span>
      </div>
      <el-button class="wb__cta" :icon="Plus" @click="openAddDialog">添加卡片</el-button>
    </div>

    <!-- 可拖拽排序的卡片网格 -->
    <draggable v-model="enabled" :item-key="(el) => el" handle=".wc__drag" :animation="150"
      class="wb__grid" @end="persist">
      <template #item="{ element }">
        <div class="wb__cell">
          <WorkbenchCard :card="catalogMap[element]" @remove="removeCard" />
        </div>
      </template>
    </draggable>

    <el-empty v-if="!enabled.length" description="工作台为空，点击「添加卡片」从分组中批量添加">
      <el-button type="primary" @click="openAddDialog">添加卡片</el-button>
    </el-empty>

    <!-- 卡片磁贴选择：分组陈列、整块可点选，一次批量加入（改进 Worktile 逐个添加） -->
    <el-dialog v-model="addDialog" title="添加卡片" width="calc(var(--mido-drawer-width) * 1.2)" class="wb__dialog">
      <div v-for="(cards, group) in grouped" :key="group" class="wb__group">
        <div class="wb__group-head">
          <span class="wb__group-title">{{ group }}</span>
          <el-button link type="primary" :disabled="groupAllAdded(cards)" @click="selectGroup(cards)">
            全选本组
          </el-button>
        </div>
        <div class="wb__tiles">
          <button v-for="c in cards" :key="c.id" type="button" class="wb__tile"
            :class="{ 'is-selected': pending.includes(c.id), 'is-added': isAdded(c.id) }"
            :disabled="isAdded(c.id)" @click="toggleCard(c.id)">
            <el-icon class="wb__tile-icon"><component :is="c.icon" /></el-icon>
            <span class="wb__tile-text">
              <span class="wb__tile-title">{{ c.title }}</span>
              <span class="wb__tile-desc">{{ c.desc }}</span>
            </span>
            <el-icon v-if="pending.includes(c.id)" class="wb__tile-check"><Select /></el-icon>
            <span v-else-if="isAdded(c.id)" class="wb__tile-added">已添加</span>
          </button>
        </div>
      </div>
      <template #footer>
        <span class="wb__footer-count">已选 {{ addable.length }} 项</span>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!addable.length" @click="confirmAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import draggable from 'vuedraggable'
import {
  Plus, Select, Folder, CircleCheck, Stamp, List, Warning, Bell,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WorkbenchCard from './workbench/WorkbenchCard.vue'
import { workbenchApi } from '@/api/workbench'
import { useMe } from '@/composables/useMe'

// 顶部问候条：时段问候 + 当前用户名 + 日期，给工作台身份感、不浪费顶部空间
const { name: myName } = useMe()
const now = new Date()
const greeting = (() => {
  const h = now.getHours()
  if (h < 6) return '凌晨好'
  if (h < 11) return '早上好'
  if (h < 13) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})()
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const todayText = `${now.getMonth() + 1}月${now.getDate()}日 ${WEEKDAYS[now.getDay()]}`

// 卡片目录（design-system §7-C）。basic=基础卡片：始终存在、不可移除。
const CATALOG = [
  { id: 'myProjects', group: '项目', title: '我参与的项目', type: 'projects', icon: Folder, desc: '我负责或参与的项目清单' },
  { id: 'pendingVerify', group: '项目', title: '待我验收的项目', type: 'pendingVerify', icon: CircleCheck, desc: '等待我做 NPSS 验收的项目' },
  { id: 'myApprovals', group: '审批', title: '待我审批的立项', type: 'approvals', icon: Stamp, desc: '需要我审批的立项申请' },
  { id: 'myTasks', group: '任务', title: '我负责的任务', type: 'tasks', icon: List, desc: '指派给我的待办任务', basic: true },
  { id: 'overdueTasks', group: '任务', title: '逾期任务预警', type: 'overdueTasks', icon: Warning, desc: '已逾期、需尽快处理的任务' },
  { id: 'myNotifications', group: '通知', title: '我的未读通知', type: 'notifications', icon: Bell, desc: '未读的系统与业务通知', basic: true },
]
const catalogMap = Object.fromEntries(CATALOG.map((c) => [c.id, c]))
// 基础卡片 id（始终存在、不可移除）
const BASIC_IDS = CATALOG.filter((c) => c.basic).map((c) => c.id)
// 默认布局：保持原有四张，新增卡片默认不开启（不破坏既有体验，可经「添加卡片」加入）
const DEFAULT_IDS = ['myProjects', 'myApprovals', 'myTasks', 'myNotifications']
const grouped = computed(() => {
  const g = {}
  CATALOG.forEach((c) => { (g[c.group] ||= []).push(c) })
  return g
})

// 确保基础卡片始终在列（缺失则补到最前），并保留原有顺序、去重
function withBasics(ids) {
  const present = ids.filter((id) => catalogMap[id])
  const missingBasics = BASIC_IDS.filter((id) => !present.includes(id))
  return [...missingBasics, ...present]
}

// 已启用卡片（有序，持久化到 pm_view）；默认含基础卡片
const enabled = ref(withBasics(DEFAULT_IDS))
// 用户在加载完成前就改动过 → 不让慢到的初始加载覆盖其改动
let userEdited = false

// 从后端布局加载；未保存过(cards=null)用默认；过滤已下线卡片 id；强制补齐基础卡片
onMounted(async () => {
  try {
    const layout = await workbenchApi.getLayout()
    const saved = layout?.cards
    // null=未保存过(用默认)；数组(含空)=用户已保存的布局，按目录过滤下线卡片并补基础卡片
    if (Array.isArray(saved) && !userEdited) {
      enabled.value = withBasics(saved)
    }
  } catch {
    // 加载失败时保留默认布局（请求层已提示），不抛未处理异常
  }
})
async function persist() {
  userEdited = true
  try {
    await workbenchApi.saveLayout(enabled.value)
  } catch {
    // 保存失败：回读后端布局，避免本地与后端长期不一致
    ElMessage.error('布局保存失败，已恢复上次保存的布局')
    try {
      const layout = await workbenchApi.getLayout()
      enabled.value = Array.isArray(layout?.cards)
        ? layout.cards.filter((id) => catalogMap[id])
        : CATALOG.map((c) => c.id)
    } catch { /* 回读也失败则维持当前界面 */ }
  }
}

const addDialog = ref(false)
const pending = ref([])
// 「卡片是否已加入工作台」唯一判定，模板与各处复用，避免谓词散落
const isAdded = (id) => enabled.value.includes(id)
const addable = computed(() => pending.value.filter((id) => !isAdded(id)))

// 每次打开都从空选开始，避免上次取消遗留的选中态
function openAddDialog() {
  pending.value = []
  addDialog.value = true
}

// 整块磁贴点选：已添加的卡片不可再选；其余在选中态间切换
function toggleCard(id) {
  if (isAdded(id)) return
  pending.value = pending.value.includes(id)
    ? pending.value.filter((x) => x !== id)
    : [...pending.value, id]
}
// 本组是否已全部加入（用于禁用「全选本组」）
function groupAllAdded(cards) {
  return cards.every((c) => isAdded(c.id))
}
function selectGroup(cards) {
  const ids = cards.filter((c) => !isAdded(c.id)).map((c) => c.id)
  pending.value = Array.from(new Set([...pending.value, ...ids]))
}
function confirmAdd() {
  enabled.value = [...enabled.value, ...addable.value]
  pending.value = []
  addDialog.value = false
  persist()
}
function removeCard(id) {
  // 基础卡片不可移除（双保险：卡片侧已隐藏关闭按钮）
  if (BASIC_IDS.includes(id)) return
  enabled.value = enabled.value.filter((x) => x !== id)
  persist()
}
</script>

<style scoped>
/* 品牌温度页头：浅色淡底 + 深色文字，弱化视觉权重（design-system §1.6 克制点睛、不抢注意力）*/
/* 问候栏：纯标题行融入页面（不再用品牌色块，避免与「白卡浮于灰底」格格不入）；
   去左右内边距使问候文字与下方卡片网格左对齐 */
.wb__hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-4);
  margin-bottom: var(--mido-space-4);
}
.wb__greeting {
  display: flex;
  align-items: baseline;
  gap: var(--mido-space-3);
}
.wb__hello {
  font-size: var(--mido-font-size-h2);
  line-height: var(--mido-line-height-h2);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
.wb__date {
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-secondary);
}
/* 浅底上的主操作：主色文字按钮，弱化为点睛而非抢眼色块 */
.wb__cta {
  flex-shrink: 0;
}
.wb__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(calc(var(--mido-drawer-width) * 0.8), 1fr));
  gap: var(--mido-space-4);
}
.wb__group {
  margin-bottom: var(--mido-space-5);
}
.wb__group:last-child {
  margin-bottom: 0;
}
.wb__group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.wb__group-title {
  font-size: var(--mido-font-size-secondary);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-secondary);
}
/* 磁贴两列等宽网格，整齐成块 */
.wb__tiles {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--mido-space-3);
}
/* 整块可点选磁贴：图标 + 标题/副标题 + 选中态角标 */
.wb__tile {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  padding: var(--mido-space-3);
  text-align: left;
  /* 原生 button 不继承字体，需显式继承，避免磁贴文字落到 UA 控件字体 */
  font: inherit;
  background: var(--el-bg-color);
  border: var(--mido-border-width) solid var(--el-border-color);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: border-color var(--mido-duration) var(--mido-ease),
    background-color var(--mido-duration) var(--mido-ease),
    box-shadow var(--mido-duration) var(--mido-ease);
}
.wb__tile:hover:not(:disabled) {
  border-color: var(--el-color-primary);
  box-shadow: var(--mido-shadow-card);
}
.wb__tile:focus-visible {
  outline: none;
  box-shadow: var(--mido-focus-ring);
}
.wb__tile.is-selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.wb__tile.is-added {
  cursor: not-allowed;
  background: var(--el-fill-color-light);
}
.wb__tile-icon {
  flex-shrink: 0;
  font-size: var(--mido-font-size-h2);
  color: var(--el-color-primary);
}
.wb__tile.is-added .wb__tile-icon {
  color: var(--el-text-color-placeholder);
}
.wb__tile-text {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
  min-width: 0;
}
.wb__tile-title {
  font-size: var(--mido-font-size-body);
  color: var(--el-text-color-primary);
}
.wb__tile.is-added .wb__tile-title {
  color: var(--el-text-color-secondary);
}
.wb__tile-desc {
  font-size: var(--mido-font-size-caption);
  line-height: var(--mido-line-height-caption);
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wb__tile-check {
  flex-shrink: 0;
  margin-left: auto;
  font-size: var(--mido-font-size-h2);
  color: var(--el-color-primary);
}
.wb__tile-added {
  flex-shrink: 0;
  margin-left: auto;
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-placeholder);
}
.wb__footer-count {
  margin-right: auto;
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-secondary);
}
.wb__dialog :deep(.el-dialog__footer) {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
</style>
