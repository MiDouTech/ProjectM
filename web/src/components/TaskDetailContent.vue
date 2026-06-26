<template>
  <!-- 任务详情内容（抽屉与独立页共用）：
       左主区（信息/子任务/工时/附件）为主、占比更大；右侧栏（评论/活动）收窄。
       embedded=true 时（抽屉内）显示「在新页打开」入口，支持直链查看。 -->
  <div v-loading="loading" class="td">
    <header class="td__head">
      <div class="td__title">
        <el-icon v-if="task.isMilestone"><Flag /></el-icon>
        <h2 class="mido-h2">{{ task.title }}</h2>
        <StatusTag :status="task.status" />
        <el-button v-if="embedded" class="td__expand" link type="primary" :icon="TopRight" @click="openInPage">在新页打开</el-button>
        <el-button v-if="task.id" link type="primary" :icon="EditPen" @click="openChange">发起变更</el-button>
      </div>
      <div class="td__meta mido-text-secondary">
        <span>负责人：{{ userName(task.assigneeId) }}</span>
        <span>优先级：{{ priorityLabel(task.priority) }}</span>
        <span>{{ task.startDate || '—' }} ~ {{ task.dueDate || '—' }}</span>
      </div>
      <!-- 状态流转（合法下一态按钮，看板外的另一入口） -->
      <div v-if="nextStatuses.length" class="td__trans">
        <span class="mido-text-secondary">流转：</span>
        <el-button v-for="s in nextStatuses" :key="s" size="small" plain type="primary"
          :disabled="isViewOnly('status')" @click="transition(s)">
          → {{ s }}
        </el-button>
      </div>
    </header>

    <div class="td__body">
      <section class="td__main">
        <el-tabs v-model="tab">
          <el-tab-pane label="信息" name="info">
            <el-form ref="formRef" :model="form" :rules="rules" :label-width="80" class="td__form">
              <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
              <el-form-item label="负责人">
                <UserSelect v-model="form.assigneeId" placeholder="选择负责人"
                  :disabled="isViewOnly('assignee')" @change="onAssign" />
              </el-form-item>
              <el-form-item label="优先级">
                <el-select v-model="form.priority" clearable placeholder="选择优先级" class="full"
                  :disabled="isViewOnly('priority')">
                  <el-option v-for="p in TASK_PRIORITIES" :key="p.value" :label="p.label" :value="p.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="阶段"><el-input v-model="form.stage" placeholder="可选" /></el-form-item>
              <el-form-item label="开始"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item>
              <el-form-item label="截止"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item>
              <el-form-item label="里程碑"><el-switch v-model="milestone" /></el-form-item>
              <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
              </el-form-item>
            </el-form>
            <CustomFieldsSection v-if="task.id" entity-type="task" :entity-id="task.id" @changed="emit('changed')" />
          </el-tab-pane>

          <el-tab-pane label="子任务" name="sub">
            <div class="td__bar">
              <span class="mido-text-secondary">共 {{ subtasks.length }} 个子任务</span>
              <el-button link type="primary" :icon="Plus" @click="subDialog = true">添加子任务</el-button>
            </div>
            <el-table :data="subtasks" class="is-clickable" @row-click="(r) => $emit('open', r.id)">
              <el-table-column label="标题" prop="title" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
              <el-table-column label="负责人" width="110">
                <template #default="{ row }">{{ userName(row.assigneeId) }}</template>
              </el-table-column>
              <template #empty><el-empty description="暂无子任务" :image-size="60" /></template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="工时" name="workhour">
            <WorkHourPanel :task-id="taskId" :user-name="userName" />
          </el-tab-pane>

          <el-tab-pane label="附件" name="attach">
            <AttachmentPanel entity-type="task" :entity-id="taskId" :user-name="userName" />
          </el-tab-pane>

          <el-tab-pane label="关联" name="relation">
            <div class="td__bar">
              <span class="mido-text-secondary">共 {{ relations.length }} 条关联（追溯链）</span>
              <el-button link type="primary" :icon="Plus" @click="openRel">添加关联</el-button>
            </div>
            <el-table :data="relations" class="is-clickable" @row-click="(r) => $emit('open', r.relatedTaskId)">
              <el-table-column label="关系" width="120">
                <template #default="{ row }">
                  {{ relationLabel(row.relationKind) }}<span class="mido-text-secondary"> · {{ row.direction === 'outgoing' ? '指向' : '来自' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="关联任务" prop="relatedTitle" min-width="160" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }"><StatusTag :status="row.relatedStatus" /></template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ row }">
                  <el-button link type="danger" @click.stop="removeRel(row)">移除</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无关联" :image-size="60" /></template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="td__side">
        <el-tabs v-model="sideTab">
          <el-tab-pane label="评论" name="comment">
            <CommentThread v-if="task.id" entity-type="task" :entity-id="task.id" :users="resolvedUsers" />
          </el-tab-pane>
          <el-tab-pane label="活动" name="activity">
            <ActivityTimeline entity-type="task" :entity-id="taskId" :user-name="userName" />
          </el-tab-pane>
        </el-tabs>
      </aside>
    </div>

    <!-- 添加子任务 -->
    <el-dialog v-model="subDialog" title="添加子任务" width="var(--mido-login-card-width)" append-to-body>
      <el-form :label-width="64">
        <el-form-item label="标题"><el-input v-model="subForm.title" /></el-form-item>
        <el-form-item label="负责人">
          <UserSelect v-model="subForm.assigneeId" placeholder="选择负责人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addSub">添加</el-button>
      </template>
    </el-dialog>

    <!-- 添加关联（同项目任务） -->
    <el-dialog v-model="relOpen" title="添加关联" width="var(--mido-login-card-width)" append-to-body>
      <el-form :label-width="64">
        <el-form-item label="关系">
          <el-select v-model="relForm.relationKind" class="full">
            <el-option v-for="k in RELATION_KINDS" :key="k.value" :label="k.label" :value="k.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标任务">
          <el-select v-model="relForm.targetTaskId" filterable placeholder="选择同项目任务" class="full">
            <el-option v-for="t in relCandidates" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRel">添加</el-button>
      </template>
    </el-dialog>

    <!-- 发起重大任务变更：受控变更，走变更中心 + 审批引擎（按变更策略必审/免审） -->
    <el-dialog v-model="changeOpen" title="发起重大任务变更" width="var(--mido-login-card-width)" append-to-body>
      <el-form ref="changeRef" :model="changeForm" :rules="changeRules" :label-width="92">
        <el-form-item label="变更事由" prop="reason">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="为什么要变更（留痕）" />
        </el-form-item>
        <el-form-item label="影响分析">
          <el-input v-model="changeForm.impact" type="textarea" :rows="2" placeholder="对计划/干系人的影响（可选）" />
        </el-form-item>
        <el-divider>拟改值（仅填需变更项）</el-divider>
        <el-form-item label="负责人">
          <UserSelect v-model="changeForm.assigneeId" placeholder="改派负责人" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="changeForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="新开始日期" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="changeForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="新截止日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeOpen = false">取消</el-button>
        <el-button type="primary" :loading="changeSaving" @click="submitChange">提交变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Flag, Plus, TopRight, EditPen } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CommentThread from '@/components/CommentThread.vue'
import ActivityTimeline from '@/components/ActivityTimeline.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import WorkHourPanel from '@/components/WorkHourPanel.vue'
import UserSelect from '@/components/UserSelect.vue'
import CustomFieldsSection from '@/components/CustomFieldsSection.vue'
import { taskApi, TASK_PRIORITIES, TASK_TRANSITIONS, relationApi, RELATION_KINDS } from '@/api/task'
import { fetchMembers, fieldPermApi } from '@/api/org'
import { userName as nameOf } from '@/utils/display'

const props = defineProps({
  taskId: { type: [Number, String], default: null },
  projectId: { type: [Number, String], default: null },
  // 调用方可传入成员用于名称解析；不传则本组件自行加载
  users: { type: Array, default: () => [] },
  // true=抽屉内嵌（展示「在新页打开」）；false=独立页
  embedded: { type: Boolean, default: false },
})
const emit = defineEmits(['changed', 'open', 'navigate'])

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const task = ref({})
const subtasks = ref([])
const tab = ref('info')
const sideTab = ref('comment')
const formRef = ref()
const form = reactive({ title: '', assigneeId: null, priority: null, stage: '', startDate: null, dueDate: null, description: '' })
const milestone = ref(false)
const rules = { title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }] }
const subDialog = ref(false)
const subForm = reactive({ title: '', assigneeId: null })

// 关联（追溯）
const relations = ref([])
const relOpen = ref(false)
const relForm = reactive({ targetTaskId: null, relationKind: 'related' })
const relCandidates = ref([])
const relationLabel = (k) => RELATION_KINDS.find((x) => x.value === k)?.label || k

// 成员：优先用调用方传入，否则自行加载（独立页场景）
const localUsers = ref([])
const resolvedUsers = computed(() => (props.users.length ? props.users : localUsers.value))
const userName = (id) => nameOf(resolvedUsers.value, id)
const priorityLabel = (p) => TASK_PRIORITIES.find((x) => x.value === p)?.label || '—'
const nextStatuses = computed(() => TASK_TRANSITIONS[task.value.status] || [])

// 字段级权限：当前用户在 task 资源下的只读字段（仅 UX，写入拦截在后端）
const viewOnlyFields = ref(new Set())
const isViewOnly = (field) => viewOnlyFields.value.has(field)

watch(() => props.taskId, (id) => { if (id) { tab.value = 'info'; sideTab.value = 'comment'; reload() } }, { immediate: true })

onMounted(async () => {
  if (!props.users.length) {
    try {
      localUsers.value = await fetchMembers()
    } catch {
      localUsers.value = []
    }
  }
  try {
    viewOnlyFields.value = new Set(await fieldPermApi.viewOnly('task'))
  } catch {
    viewOnlyFields.value = new Set()
  }
})

async function reload() {
  if (!props.taskId) return
  loading.value = true
  try {
    const [detail, subs, rels] = await Promise.all([
      taskApi.get(props.taskId),
      taskApi.subtasks(props.taskId),
      relationApi.list(props.taskId),
    ])
    task.value = detail
    Object.assign(form, {
      title: detail.title, assigneeId: detail.assigneeId, priority: detail.priority,
      stage: detail.stage, startDate: detail.startDate, dueDate: detail.dueDate,
      description: detail.description,
    })
    milestone.value = detail.isMilestone === 1
    subtasks.value = subs
    relations.value = rels || []
  } finally {
    loading.value = false
  }
}

async function loadRelations() {
  relations.value = (await relationApi.list(props.taskId)) || []
}
async function openRel() {
  relForm.targetTaskId = null
  relForm.relationKind = 'related'
  // 候选：同项目任务（排除自身）
  const res = await taskApi.query({ projectId: task.value.projectId, page: 1, size: 200 })
  relCandidates.value = (res.list || []).filter((t) => t.id !== task.value.id)
  relOpen.value = true
}
async function saveRel() {
  if (!relForm.targetTaskId) return ElMessage.warning('请选择目标任务')
  saving.value = true
  try {
    await relationApi.add(task.value.id, { targetTaskId: relForm.targetTaskId, relationKind: relForm.relationKind })
    ElMessage.success('已添加关联')
    relOpen.value = false
    loadRelations()
  } finally {
    saving.value = false
  }
}
async function removeRel(row) {
  await ElMessageBox.confirm('确认移除该关联？', '提示', { type: 'warning' })
  await relationApi.remove(task.value.id, row.id)
  ElMessage.success('已移除')
  loadRelations()
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await taskApi.update(task.value.id, {
      title: form.title, priority: form.priority, stage: form.stage,
      startDate: form.startDate, dueDate: form.dueDate,
      isMilestone: milestone.value ? 1 : 0, description: form.description,
    })
    ElMessage.success('已保存')
    emit('changed')
    reload()
  } finally {
    saving.value = false
  }
}
async function onAssign(val) {
  await taskApi.assign(task.value.id, val ?? null)
  ElMessage.success('已更新负责人')
  emit('changed')
}
async function transition(s) {
  await taskApi.transition(task.value.id, s)
  ElMessage.success(`已流转至「${s}」`)
  emit('changed')
  reload()
}

// 发起重大任务变更
const changeOpen = ref(false)
const changeSaving = ref(false)
const changeRef = ref()
const changeForm = reactive({ reason: '', impact: '', assigneeId: null, startDate: '', dueDate: '' })
const changeRules = {
  reason: [{ required: true, message: '请填写变更事由', trigger: 'blur' }],
}
function openChange() {
  Object.assign(changeForm, {
    reason: '', impact: '',
    assigneeId: task.value.assigneeId ?? null,
    startDate: task.value.startDate || '', dueDate: task.value.dueDate || '',
  })
  changeOpen.value = true
}
async function submitChange() {
  await changeRef.value.validate()
  changeSaving.value = true
  try {
    await taskApi.submitChange(task.value.id, { changeType: 'task_baseline', ...changeForm })
    ElMessage.success('变更已提交')
    changeOpen.value = false
    emit('changed')
    reload()
  } finally {
    changeSaving.value = false
  }
}
async function addSub() {
  if (!subForm.title.trim()) return ElMessage.warning('请输入标题')
  saving.value = true
  try {
    await taskApi.create({ title: subForm.title, projectId: props.projectId, parentId: task.value.id, assigneeId: subForm.assigneeId })
    ElMessage.success('已添加子任务')
    subDialog.value = false
    subForm.title = ''
    subForm.assigneeId = null
    emit('changed')
    reload()
  } finally {
    saving.value = false
  }
}
function openInPage() {
  router.push(`/project/${props.projectId}/task/${props.taskId}`)
  emit('navigate')
}
</script>

<style scoped>
.td {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.td__head {
  padding-bottom: var(--mido-space-3);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.td__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.td__expand {
  margin-left: auto;
}
.td__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
  margin-top: var(--mido-space-2);
}
.td__trans {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-3);
}
.td__body {
  display: flex;
  gap: var(--mido-space-5);
  flex: 1;
  min-height: 0;
  margin-top: var(--mido-space-3);
}
.td__main {
  /* 主区占比更大：信息/子任务/工时/附件是工作重心 */
  flex: 1;
  min-width: 0;
}
.td__form {
  max-width: 560px;
}
.td__side {
  /* 右侧评论/活动收窄为辅助栏 */
  width: 340px;
  flex: none;
  border-left: var(--mido-border-width) solid var(--el-border-color-light);
  padding-left: var(--mido-space-5);
}
.td__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.full {
  width: 100%;
}
</style>
