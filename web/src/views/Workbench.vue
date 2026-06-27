<template>
  <div class="mido-page">
    <div class="wb__bar">
      <el-button type="primary" :icon="Plus" @click="addDialog = true">添加卡片</el-button>
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
      <el-button type="primary" @click="addDialog = true">添加卡片</el-button>
    </el-empty>

    <!-- 分组批量添加（改进 Worktile 逐个添加：按分组多选一次性加入） -->
    <el-dialog v-model="addDialog" title="添加卡片" width="var(--mido-drawer-width)">
      <el-checkbox-group v-model="pending">
        <div v-for="(cards, group) in grouped" :key="group" class="wb__group">
          <div class="wb__group-head">
            <span class="mido-h2">{{ group }}</span>
            <el-button link type="primary" @click="selectGroup(cards)">全选本组</el-button>
          </div>
          <div class="wb__group-body">
            <el-checkbox v-for="c in cards" :key="c.id" :value="c.id" :disabled="enabled.includes(c.id)">
              {{ c.title }}<span v-if="enabled.includes(c.id)" class="mido-text-secondary">（已添加）</span>
            </el-checkbox>
          </div>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!addable.length" @click="confirmAdd">
          添加 {{ addable.length || '' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import draggable from 'vuedraggable'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WorkbenchCard from './workbench/WorkbenchCard.vue'
import { workbenchApi } from '@/api/workbench'

// 卡片目录（design-system §7-C）。basic=基础卡片：始终存在、不可移除。
const CATALOG = [
  { id: 'myProjects', group: '项目', title: '我参与的项目', type: 'projects' },
  { id: 'pendingVerify', group: '项目', title: '待我验收的项目', type: 'pendingVerify' },
  { id: 'myApprovals', group: '审批', title: '待我审批的立项', type: 'approvals' },
  { id: 'myTasks', group: '任务', title: '我负责的任务', type: 'tasks', basic: true },
  { id: 'overdueTasks', group: '任务', title: '逾期任务预警', type: 'overdueTasks' },
  { id: 'myNotifications', group: '通知', title: '我的未读通知', type: 'notifications', basic: true },
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
const addable = computed(() => pending.value.filter((id) => !enabled.value.includes(id)))

function selectGroup(cards) {
  const ids = cards.map((c) => c.id)
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
.wb__bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: var(--mido-space-4);
}
.wb__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(calc(var(--mido-drawer-width) * 0.8), 1fr));
  gap: var(--mido-space-4);
}
.wb__group {
  margin-bottom: var(--mido-space-4);
}
.wb__group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.wb__group-body {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
}
</style>
