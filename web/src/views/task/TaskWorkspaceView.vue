<template>
  <div class="mido-page">
    <div class="tw__bar">
      <div class="tw__bar-left">
        <el-button :icon="ArrowLeft" link @click="$router.push('/project')">返回项目</el-button>
        <h1 class="mido-h1">{{ project.name || '任务' }}</h1>
        <CategoryBadge v-if="project.category" :category="project.category" :show-label="false" />
        <ViewSwitcher v-model="view" :views="VIEWS" />
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
    </div>

    <el-card shadow="never" v-loading="loading">
      <!-- 看板 -->
      <KanbanBoard v-if="view === 'board'" :columns="columns" @change="onDrag" @open="(t) => openDetail(t.id)">
        <template #card="{ task }">
          <div class="tc">
            <div class="tc__title">
              <el-icon v-if="task.isMilestone"><Flag /></el-icon>
              <span>{{ task.title }}</span>
            </div>
            <div class="tc__foot mido-text-secondary">
              <span :class="{ 'tc__overdue': isOverdue(task) }">{{ task.dueDate || '无截止' }}</span>
              <span>{{ priorityLabel(task.priority) }} · {{ userName(task.assigneeId) }}</span>
            </div>
          </div>
        </template>
      </KanbanBoard>

      <!-- 列表（可展开子任务） -->
      <el-table v-else :data="tree" row-key="id" :tree-props="{ children: 'children' }"
        default-expand-all @row-click="(r) => openDetail(r.id)">
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
        <el-table-column label="截止" width="130">
          <template #default="{ row }">
            <span :class="{ 'tc__overdue': isOverdue(row) }">{{ row.dueDate || '—' }}</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无任务，点击右上角新建任务" /></template>
      </el-table>
    </el-card>

    <!-- 新建任务 -->
    <el-dialog v-model="createDialog" title="新建任务" width="var(--mido-login-card-width)">
      <el-form ref="createRef" :model="createForm" :rules="createRules" :label-width="72">
        <el-form-item label="标题" prop="title"><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="createForm.assigneeId" filterable clearable placeholder="选择负责人" class="full">
            <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Flag } from '@element-plus/icons-vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import KanbanBoard from '@/components/KanbanBoard.vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import TaskDetailDrawer from './TaskDetailDrawer.vue'
import { taskApi, TASK_STATUSES, TASK_PRIORITIES } from '@/api/task'
import { projectApi } from '@/api/project'
import { userApi } from '@/api/org'

const VIEWS = [
  { value: 'board', label: '看板' },
  { value: 'list', label: '列表' },
]

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false)
const saving = ref(false)
const view = ref('board')
const project = ref({})
const tasks = ref([])
const columns = ref(TASK_STATUSES.map((s) => ({ status: s, tasks: [] })))
const users = ref([])

const detailOpen = ref(false)
const detailId = ref(null)
const createDialog = ref(false)
const createRef = ref()
const createForm = reactive({ title: '', assigneeId: null, priority: null, dueDate: null, isMilestone: false })
const createRules = { title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }] }

const userName = (id) => users.value.find((u) => u.id === id)?.name || (id ? `用户#${id}` : '—')
const priorityLabel = (p) => TASK_PRIORITIES.find((x) => x.value === p)?.label || '—'
const today = new Date().toISOString().slice(0, 10)
const isOverdue = (t) => t.dueDate && t.dueDate < today && t.status !== '已完成' && t.status !== '已验收'

// 列表树：扁平任务 → parentId 归并
const tree = computed(() => {
  const map = new Map(tasks.value.map((t) => [t.id, { ...t, children: [] }]))
  const roots = []
  for (const node of map.values()) {
    const parent = node.parentId ? map.get(node.parentId) : null
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  // 无子任务的 children 置空以免出现展开箭头
  for (const n of map.values()) if (!n.children.length) delete n.children
  return roots
})

async function load() {
  loading.value = true
  try {
    const cols = await taskApi.kanban(projectId)
    // 后端列可能缺省，按固定状态序补齐
    const byStatus = Object.fromEntries(cols.map((c) => [c.status, c.tasks || []]))
    columns.value = TASK_STATUSES.map((s) => ({ status: s, tasks: byStatus[s] || [] }))
    const res = await taskApi.query({ projectId, page: 1, size: 500 })
    tasks.value = res.list || []
  } finally {
    loading.value = false
  }
}

async function onDrag({ task, toStatus }) {
  try {
    await taskApi.transition(task.id, toStatus)
    // 成功：同步卡片状态（否则再次拖拽时 :move 会按旧状态误判），并刷新列表树
    task.status = toStatus
    const res = await taskApi.query({ projectId, page: 1, size: 500 })
    tasks.value = res.list || []
  } catch {
    // 失败（非法流转等）：以后端为准回滚看板
    load()
  }
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
  project.value = await projectApi.get(projectId)
  const res = await userApi.query({ page: 1, size: 200 })
  users.value = res.list || []
  load()
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
.tc__overdue {
  color: var(--el-color-danger);
}
.full {
  width: 100%;
}
</style>
