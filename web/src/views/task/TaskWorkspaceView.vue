<template>
  <div :class="{ 'mido-page': !embedded }">
    <div class="tw__bar">
      <div class="tw__bar-left">
        <template v-if="!embedded">
          <el-button :icon="ArrowLeft" link @click="$router.push('/project')">返回项目</el-button>
          <h1 class="mido-h1">{{ project.name || '任务' }}</h1>
          <CategoryBadge v-if="project.category" :category="project.category" :show-label="false" />
        </template>
        <ViewSwitcher v-model="view" :views="VIEWS" @update:model-value="onSwitchType" />
        <el-select v-model="activeViewId" placeholder="我的视图" clearable class="tw__viewsel"
          @change="onSelectView">
          <el-option v-for="v in savedViews" :key="v.id" :label="v.name" :value="v.id" />
        </el-select>
        <el-button link type="primary" :icon="Plus" @click="designerOpen = true">新建视图</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
    </div>

    <el-card shadow="never" v-loading="loading">
      <!-- 看板 -->
      <KanbanBoard v-if="view === 'board'" :columns="columns" :disabled="transitioning"
        :can-move="canMove" @change="onDrag" @open="(t) => openDetail(t.id)">
        <template #card="{ task }">
          <div class="tc">
            <div class="tc__title">
              <el-icon v-if="task.isMilestone"><Flag /></el-icon>
              <span>{{ task.title }}</span>
            </div>
            <div class="tc__foot mido-text-secondary">
              <span class="tc__due">
                {{ task.dueDate || '无截止' }}
                <StatusTag v-if="isOverdue(task)" status="逾期" />
              </span>
              <span>{{ priorityLabel(task.priority) }} · {{ userName(task.assigneeId) }}</span>
            </div>
          </div>
        </template>
      </KanbanBoard>

      <!-- 视图设计器结果（分组/排序/筛选后的任务） -->
      <div v-else-if="view === 'view'">
        <el-alert v-if="groupedTotal >= 500" type="warning" :closable="false" show-icon
          title="结果较多，仅显示前 500 条，请收窄筛选条件" class="tw__cap" />
        <el-collapse v-if="grouped.groups.length">
          <el-collapse-item v-for="(g, i) in grouped.groups" :key="i" :name="i"
            :title="`${groupTitle(g.groupKey)}（${g.tasks.length}）`">
            <el-table :data="g.tasks" class="is-clickable" @row-click="(r) => openDetail(r.id)">
              <el-table-column label="标题" min-width="220">
                <template #default="{ row }">
                  <span class="tc__title"><el-icon v-if="row.isMilestone"><Flag /></el-icon>{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
              <el-table-column label="负责人" width="120">
                <template #default="{ row }">{{ userName(row.assigneeId) }}</template>
              </el-table-column>
              <el-table-column label="优先级" width="90">
                <template #default="{ row }">{{ priorityLabel(row.priority) }}</template>
              </el-table-column>
              <el-table-column label="截止" width="130" prop="dueDate" sortable />
              <el-table-column v-for="c in cfColumns" :key="c.value" :label="c.def.name" min-width="120">
                <template #default="{ row }">{{ formatCf(c, row.customFields) }}</template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-else description="该视图暂无匹配任务" />
      </div>

      <!-- 列表（可展开子任务 + 多选批量操作） -->
      <div v-else>
        <div v-if="selectedIds.length" class="tw__batch">
          <span class="mido-text-secondary">已选 {{ selectedIds.length }} 项</span>
          <el-dropdown :disabled="batching" @command="batchSetStatus">
            <el-button :loading="batching">批量改状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="s in TASK_STATUSES" :key="s" :command="s">{{ s }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <UserSelect v-model="batchAssignee" placeholder="批量改负责人"
            class="tw__batch-sel" :disabled="batching" @change="batchSetAssignee" />
          <el-button type="danger" plain :loading="batching" @click="batchRemove">批量删除</el-button>
        </div>
        <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }"
          default-expand-all class="is-clickable" @row-click="(r) => openDetail(r.id)" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <span class="tc__title">
              <el-icon v-if="row.isMilestone"><Flag /></el-icon>{{ row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">{{ userName(row.assigneeId) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">{{ priorityLabel(row.priority) }}</template>
        </el-table-column>
        <el-table-column label="截止" width="150">
          <template #default="{ row }">
            {{ row.dueDate || '—' }}
            <StatusTag v-if="isOverdue(row)" status="逾期" />
          </template>
        </el-table-column>
          <template #empty><el-empty description="暂无任务，点击右上角新建任务" /></template>
        </el-table>
      </div>
    </el-card>

    <!-- 新建任务 -->
    <el-dialog v-model="createDialog" title="新建任务" width="var(--mido-login-card-width)">
      <el-form ref="createRef" :model="createForm" :rules="createRules" :label-width="72">
        <el-form-item label="标题" prop="title"><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item label="负责人">
          <UserSelect v-model="createForm.assigneeId" placeholder="选择负责人" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" clearable placeholder="选择优先级" class="full">
            <el-option v-for="p in TASK_PRIORITIES" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止"><el-date-picker v-model="createForm.dueDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item>
        <el-form-item label="里程碑"><el-switch v-model="createForm.isMilestone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <TaskDetailDrawer v-model="detailOpen" :task-id="detailId" :project-id="projectId"
      :users="users" @changed="load" @open="openDetail" />

    <ViewDesigner v-model="designerOpen" :project-id="projectId" :cf-fields="cfFieldList" @saved="loadViews" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Flag, ArrowDown } from '@element-plus/icons-vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import KanbanBoard from '@/components/KanbanBoard.vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import TaskDetailDrawer from './TaskDetailDrawer.vue'
import ViewDesigner from '@/components/ViewDesigner.vue'
import UserSelect from '@/components/UserSelect.vue'
import { taskApi, TASK_STATUSES, TASK_PRIORITIES, TASK_TRANSITIONS } from '@/api/task'
import { viewApi } from '@/api/view'
import { fieldDefApi, isCfRef, cfKey } from '@/api/field'
import { projectApi } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { isTaskOverdue, userName as nameOf } from '@/utils/display'

const VIEWS = [
  { value: 'board', label: '看板' },
  { value: 'list', label: '列表' },
]

// 内嵌模式：作为「项目工作台」子标签渲染，隐藏自带页头（返回/标题），projectId 由父级透传
const props = defineProps({
  embedded: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: null },
})

const route = useRoute()
// 雪花 ID 为字符串，禁止 Number() 转换（会丢精度），直接透传给后端
const projectId = props.projectId ?? route.params.projectId

const loading = ref(false)
const saving = ref(false)
const view = ref('board')
const project = ref({})
const tasks = ref([])
const columns = ref(TASK_STATUSES.map((s) => ({ status: s, tasks: [] })))
const users = ref([])

// 视图设计器：保存的视图 + 当前应用视图 + 分组结果
const savedViews = ref([])
const activeViewId = ref(null)
const designerOpen = ref(false)
const grouped = ref({ groupBy: null, groups: [], columns: [] })
// 任务级自定义字段定义；列表用于透传给设计器（避免重复请求），map 用于视图 cf 列渲染
const cfFieldList = ref([])
const cfDefs = computed(() => Object.fromEntries(cfFieldList.value.map((d) => [d.fieldKey, d])))
// 当前视图 columns 中的自定义字段列（cf:<key> 且定义存在）
const cfColumns = computed(() => (grouped.value.columns || [])
  .filter((c) => isCfRef(c) && cfDefs.value[cfKey(c)])
  .map((c) => ({ value: c, key: cfKey(c), def: cfDefs.value[cfKey(c)] })))

/** 按字段类型格式化 cf 原始值用于列表展示 */
function formatCf(col, customFields) {
  const raw = customFields ? customFields[col.key] : null
  if (raw === null || raw === undefined || raw === '') return '—'
  const def = col.def
  if (def.type === 'checkbox') return raw === 'true' ? '是' : '否'
  if (def.type === 'user') return userName(Number(raw))
  if (def.type === 'select') return (def.options || []).find((o) => o.value === raw)?.label || raw
  if (def.type === 'multi_select') {
    let vals = []
    try { vals = JSON.parse(raw) } catch { vals = [] }
    return vals.map((v) => (def.options || []).find((o) => o.value === v)?.label || v).join('、') || '—'
  }
  return raw
}

const detailOpen = ref(false)
const detailId = ref(null)
const transitioning = ref(false)
const createDialog = ref(false)
const createRef = ref()
const createForm = reactive({ title: '', assigneeId: null, priority: null, dueDate: null, isMilestone: false })
const createRules = { title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }] }

const userName = (id) => nameOf(users.value, id)
const priorityLabel = (p) => TASK_PRIORITIES.find((x) => x.value === p)?.label || '—'
const isOverdue = (t) => isTaskOverdue(t)
// 注入看板的跨列移动合法性（默认工作流）；KanbanBoard 本身保持域无关
const canMove = (from, to, el) => (TASK_TRANSITIONS[el?.status] || []).includes(to)

// 列表树：扁平任务 → parentId 归并。children 仅在有子任务时惰性创建，叶子不带该键（避免多余展开箭头）
const tree = computed(() => {
  const map = new Map(tasks.value.map((t) => [t.id, { ...t }]))
  const roots = []
  for (const node of map.values()) {
    const parent = node.parentId ? map.get(node.parentId) : null
    if (parent) (parent.children ||= []).push(node)
    else roots.push(node)
  }
  return roots
})

async function load() {
  loading.value = true
  try {
    // 看板与列表数据相互独立，并行拉取
    const [cols, res] = await Promise.all([
      taskApi.kanban(projectId),
      taskApi.query({ projectId, page: 1, size: 500 }),
    ])
    // 后端列可能缺省，按固定状态序补齐
    const byStatus = Object.fromEntries(cols.map((c) => [c.status, c.tasks || []]))
    columns.value = TASK_STATUSES.map((s) => ({ status: s, tasks: byStatus[s] || [] }))
    tasks.value = res.list || []
  } finally {
    loading.value = false
  }
}

async function loadViews() {
  savedViews.value = await viewApi.list(projectId)
}
// 切换内置看板/列表时，清除已选保存视图
function onSwitchType() {
  activeViewId.value = null
}
// 选中保存视图：按其 viewId 拉分组结果，切到"视图"渲染
async function onSelectView(id) {
  if (!id) {
    if (view.value === 'view') view.value = 'board'
    return
  }
  const res = await taskApi.viewQuery({ projectId, viewId: id })
  grouped.value = res
  view.value = 'view'
}
const groupedTotal = computed(() =>
  grouped.value.groups.reduce((n, g) => n + (g.tasks?.length || 0), 0))
function groupTitle(key) {
  if (key === null || key === undefined || key === '') return '全部'
  if (grouped.value.groupBy === 'assigneeId') return userName(key)
  if (grouped.value.groupBy === 'priority') return priorityLabel(key)
  return String(key)
}

async function onDrag({ task, toStatus }) {
  transitioning.value = true
  try {
    await taskApi.transition(task.id, toStatus)
    // 成功：就地同步看板卡片与列表树数据源的状态，无需整表重拉
    task.status = toStatus
    const t = tasks.value.find((x) => x.id === task.id)
    if (t) t.status = toStatus
  } catch {
    // 失败（非法流转等）：以后端为准回滚看板
    load()
  } finally {
    transitioning.value = false
  }
}

// ===== 批量操作（列表多选）=====
const selectedIds = ref([])
const batchAssignee = ref(null)
const batching = ref(false)

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}
async function runBatch(fn, successMsg) {
  batching.value = true
  try {
    await fn()
    ElMessage.success(successMsg)
    selectedIds.value = []
    await load()
  } finally {
    batching.value = false
  }
}
function batchSetStatus(status) {
  // 非法流转由后端逐条校验，整批回滚并 toast（request 拦截器统一提示）
  runBatch(() => taskApi.batchTransition(selectedIds.value, status), '已批量改状态')
}
function batchSetAssignee(assigneeId) {
  if (assigneeId == null) return
  const ids = selectedIds.value
  runBatch(() => taskApi.batchAssign(ids, assigneeId), '已批量改负责人')
    .finally(() => { batchAssignee.value = null })
}
async function batchRemove() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个任务？`, '提示', { type: 'warning' })
  runBatch(() => taskApi.batchDelete(selectedIds.value), '已批量删除')
}

function openDetail(id) {
  detailId.value = id
  detailOpen.value = true
}
function openCreate() {
  Object.assign(createForm, { title: '', assigneeId: null, priority: null, dueDate: null, isMilestone: false })
  createDialog.value = true
}
async function doCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await taskApi.create({
      title: createForm.title, projectId, assigneeId: createForm.assigneeId,
      priority: createForm.priority, dueDate: createForm.dueDate,
      isMilestone: createForm.isMilestone ? 1 : 0,
    })
    ElMessage.success('已创建')
    createDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [proj, members] = await Promise.all([projectApi.get(projectId), fetchMembers()])
  project.value = proj
  users.value = members
  load()
  loadViews()
  try {
    cfFieldList.value = await fieldDefApi.list('task', true)
  } catch {
    cfFieldList.value = []
    ElMessage.warning('自定义字段加载失败，视图自定义列可能不显示')
  }
})
</script>

<style scoped>
.tw__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.tw__bar-left {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
}
.tw__viewsel {
  width: var(--mido-admin-nav-width);
}
.tc {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
}
.tc__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-1);
  font-weight: var(--mido-font-weight-bold);
}
.tc__foot {
  display: flex;
  justify-content: space-between;
  font-size: var(--mido-font-size-caption);
}
.tc__due {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
}
.full {
  width: 100%;
}
.tw__batch {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-3);
}
.tw__batch-sel {
  width: var(--mido-admin-nav-width);
}
</style>
