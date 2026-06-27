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
      <div class="tw__bar-right">
        <TableColumnSetting v-if="view !== 'board' && view !== 'view'" list-key="tasks"
          :all-columns="TASK_COLUMNS" :default-columns="TASK_DEFAULT_COLS" @change="onTaskColsChange" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
      </div>
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
                <template #default="{ row }"><StatusTag :status="row.status" :color="statusColor(row.status)" /></template>
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
          <el-table-column v-for="key in taskCols" :key="key" :label="taskColLabel(key)"
            :width="taskColWidth(key)" :min-width="taskColMinWidth(key)"
            :fixed="taskFrozen.includes(key) ? 'left' : false">
            <template #default="{ row }">
              <span v-if="key === 'title'" class="tc__title">
                <el-icon v-if="row.isMilestone"><Flag /></el-icon>{{ row.title }}
              </span>
              <StatusTag v-else-if="key === 'status'" :status="row.status" :color="statusColor(row.status)" />
              <span v-else-if="key === 'assigneeId'">{{ userName(row.assigneeId) }}</span>
              <span v-else-if="key === 'priority'">{{ priorityLabel(row.priority) }}</span>
              <template v-else-if="key === 'dueDate'">
                {{ row.dueDate || '—' }}
                <StatusTag v-if="isOverdue(row)" status="逾期" />
              </template>
              <span v-else-if="key === 'startDate'">{{ row.startDate || '—' }}</span>
              <span v-else-if="key === 'stage'">{{ row.stage || '—' }}</span>
              <span v-else>{{ row[key] ?? '—' }}</span>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无任务，点击右上角新建任务" /></template>
        </el-table>
      </div>
    </el-card>

    <!-- 新建任务（有页面配置走动态表单，否则回落原表单 fail-safe） -->
    <el-dialog v-model="createDialog" title="新建任务" width="var(--mido-login-card-width)">
      <DynamicForm v-if="usePageConfig" ref="dynRef" :fields="pageFields" :model-value="createForm"
        :layout="pageLayout" />
      <el-form v-else ref="createRef" :model="createForm" :rules="createRules" :label-width="72">
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
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import DynamicForm from '@/components/DynamicForm.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useStatusColors } from '@/composables/useStatusColors'
import CategoryBadge from '@/components/CategoryBadge.vue'
import TaskDetailDrawer from './TaskDetailDrawer.vue'
import ViewDesigner from '@/components/ViewDesigner.vue'
import UserSelect from '@/components/UserSelect.vue'
import { taskApi, TASK_STATUSES, TASK_PRIORITIES, TASK_TRANSITIONS } from '@/api/task'
import { viewApi, pageConfigApi } from '@/api/view'
import { fieldDefApi, fieldValueApi, isCfRef, cfKey } from '@/api/field'
import { parseFieldOptions } from '@/utils/pageConfig'
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
const { statusColor } = useStatusColors()

const loading = ref(false)
const saving = ref(false)
const view = ref('board')

// 任务列表（树形表）表头设置
const TASK_COLUMNS = [
  { key: 'title', label: '标题', required: true },
  { key: 'status', label: '状态' },
  { key: 'assigneeId', label: '负责人' },
  { key: 'priority', label: '优先级' },
  { key: 'dueDate', label: '截止时间' },
  { key: 'startDate', label: '开始时间' },
  { key: 'stage', label: '阶段' },
]
const TASK_DEFAULT_COLS = ['title', 'status', 'assigneeId', 'priority', 'dueDate']
const TASK_COL_META = {
  title: { minWidth: 240 },
  status: { width: 110 },
  assigneeId: { width: 120 },
  priority: { width: 90 },
  dueDate: { width: 150 },
  startDate: { width: 120 },
  stage: { width: 110 },
}
const taskCols = ref([...TASK_DEFAULT_COLS])
const taskFrozen = ref([])
const taskColLabel = (key) => TASK_COLUMNS.find((c) => c.key === key)?.label || key
const taskColWidth = (key) => TASK_COL_META[key]?.width
const taskColMinWidth = (key) => TASK_COL_META[key]?.minWidth
function onTaskColsChange({ columns: cols, frozen }) {
  taskCols.value = cols
  taskFrozen.value = frozen
}

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

// L3.1c 可配置建单表单（仅编排可提交的内置字段；无配置回落原表单 fail-safe）
const dynRef = ref()
const usePageConfig = ref(false)
const pageFields = ref([])
const pageLayout = ref({ columns: 1 })
const TASK_FORM_BUILTIN = {
  title: { label: '标题', type: 'text', required: true },
  assigneeId: { label: '负责人', type: 'user' },
  priority: { label: '优先级', type: 'select', options: TASK_PRIORITIES },
  dueDate: { label: '截止', type: 'date' },
  isMilestone: { label: '里程碑', type: 'checkbox' },
}
async function loadPageConfig(customDefs) {
  try {
    // customDefs 由 onMounted 预取（与视图自定义列共用一次请求）；未传则自取，避免重复拉取
    const cfg = await pageConfigApi.get('task', 'form')
    const defs = customDefs ?? await fieldDefApi.list('task', true).catch(() => [])
    const byKey = new Map((defs || []).map((d) => [d.fieldKey, d]))
    const fields = (cfg?.fields || []).map((f) => {
      if (f.source === 'builtin' && TASK_FORM_BUILTIN[f.fieldKey]) {
        const b = TASK_FORM_BUILTIN[f.fieldKey]
        return {
          fieldKey: f.fieldKey, source: 'builtin', label: b.label, type: b.type, options: b.options,
          required: f.required ?? b.required ?? false, readonly: !!f.readonly, width: f.width, group: f.group || '',
        }
      }
      if (f.source === 'custom' && byKey.has(f.fieldKey)) {
        const d = byKey.get(f.fieldKey)
        return {
          fieldKey: f.fieldKey, source: 'custom', fieldId: d.id, label: d.name, type: d.type,
          options: parseFieldOptions(d.options), required: f.required ?? d.required === 1, readonly: !!f.readonly,
          width: f.width, group: f.group || '',
        }
      }
      return null
    }).filter(Boolean)
    // 安全兜底：配置漏掉标题(提交必需)则不启用动态表单，回落原表单
    if (fields.length && fields.some((f) => f.fieldKey === 'title')) {
      pageFields.value = fields
      pageLayout.value = cfg.layout || { columns: 1 }
      usePageConfig.value = true
    } else {
      usePageConfig.value = false
    }
  } catch {
    usePageConfig.value = false
  }
}

const userName = (id) => nameOf(users.value, id)
const priorityLabel = (p) => TASK_PRIORITIES.find((x) => x.value === p)?.label || '—'
const isOverdue = (t) => isTaskOverdue(t)
// 注入看板的跨列移动合法性（默认工作流）；KanbanBoard 本身保持域无关
// 默认工作流状态用前端预判；自定义状态(不在默认表)放行拖拽，由后端工作流引擎校验
const canMove = (from, to, el) => {
  const allowed = TASK_TRANSITIONS[el?.status]
  return allowed ? allowed.includes(to) : true
}

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
  // 动态表单含自定义字段时，初始化其键，确保双绑
  if (usePageConfig.value) {
    pageFields.value.filter((f) => f.source === 'custom').forEach((f) => { createForm[f.fieldKey] = null })
  }
  createDialog.value = true
}
async function doCreate() {
  await (usePageConfig.value ? dynRef.value.validate() : createRef.value.validate())
  saving.value = true
  try {
    const id = await taskApi.create({
      title: createForm.title, projectId, assigneeId: createForm.assigneeId,
      priority: createForm.priority, dueDate: createForm.dueDate,
      isMilestone: createForm.isMilestone ? 1 : 0,
    })
    // 自定义字段值落库（仅配置含自定义字段时）；失败仅告警，不影响建单成功
    await saveCustomFieldValues(id)
    ElMessage.success('已创建')
    createDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function saveCustomFieldValues(taskId) {
  if (!usePageConfig.value || !taskId) return
  const values = pageFields.value
    .filter((f) => f.source === 'custom' && f.fieldId)
    .map((f) => {
      const v = createForm[f.fieldKey]
      return { fieldId: f.fieldId, value: Array.isArray(v) ? JSON.stringify(v) : v }
    })
    .filter((v) => v.value != null && v.value !== '')
  if (!values.length) return
  try {
    await fieldValueApi.save({ entityType: 'task', entityId: taskId, values })
  } catch {
    ElMessage.warning('任务已创建，但部分自定义字段值未保存，可在详情补填')
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
  // 复用上面已取的自定义字段列表，避免页面配置再发一次相同请求
  loadPageConfig(cfFieldList.value)
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
.tw__bar-right {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
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
