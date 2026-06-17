<template>
  <el-drawer v-model="visible" :size="'calc(var(--mido-drawer-width) * 1.6)'" :with-header="false">
    <div v-loading="loading" class="td">
      <header class="td__head">
        <div class="td__title">
          <el-icon v-if="task.isMilestone"><Flag /></el-icon>
          <h2 class="mido-h2">{{ task.title }}</h2>
          <StatusTag :status="task.status" />
        </div>
        <div class="td__meta mido-text-secondary">
          <span>负责人：{{ userName(task.assigneeId) }}</span>
          <span>优先级：{{ priorityLabel(task.priority) }}</span>
          <span>{{ task.startDate || '—' }} ~ {{ task.dueDate || '—' }}</span>
        </div>
        <!-- 状态流转（合法下一态按钮，看板外的另一入口） -->
        <div v-if="nextStatuses.length" class="td__trans">
          <span class="mido-text-secondary">流转：</span>
          <el-button v-for="s in nextStatuses" :key="s" size="small" plain type="primary" @click="transition(s)">
            → {{ s }}
          </el-button>
        </div>
      </header>

      <div class="td__body">
        <section class="td__main">
          <el-tabs v-model="tab">
            <el-tab-pane label="信息" name="info">
              <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
                <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
                <el-form-item label="负责人">
                  <el-select v-model="form.assigneeId" filterable clearable placeholder="选择负责人" class="full" @change="onAssign">
                    <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="优先级">
                  <el-select v-model="form.priority" clearable placeholder="选择优先级" class="full">
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
            </el-tab-pane>

            <el-tab-pane label="子任务" name="sub">
              <div class="td__bar">
                <span class="mido-text-secondary">共 {{ subtasks.length }} 个子任务</span>
                <el-button link type="primary" :icon="Plus" @click="subDialog = true">添加子任务</el-button>
              </div>
              <el-table :data="subtasks" @row-click="(r) => $emit('open', r.id)">
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

            <el-tab-pane label="附件" name="attach">
              <el-empty description="附件上传随文档/存储模块前端接入" />
            </el-tab-pane>
          </el-tabs>
        </section>

        <aside class="td__side">
          <el-tabs v-model="sideTab">
            <el-tab-pane label="评论" name="comment">
              <CommentThread v-if="task.id" entity-type="task" :entity-id="task.id" :users="users" />
            </el-tab-pane>
            <el-tab-pane label="活动" name="activity">
              <el-empty description="活动日志随协作模块前端接入" />
            </el-tab-pane>
          </el-tabs>
        </aside>
      </div>
    </div>

    <!-- 添加子任务 -->
    <el-dialog v-model="subDialog" title="添加子任务" width="var(--mido-login-card-width)" append-to-body>
      <el-form :label-width="64">
        <el-form-item label="标题"><el-input v-model="subForm.title" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="subForm.assigneeId" filterable clearable placeholder="选择负责人" class="full">
            <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addSub">添加</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Flag, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CommentThread from '@/components/CommentThread.vue'
import { taskApi, TASK_PRIORITIES, TASK_TRANSITIONS } from '@/api/task'
import { userName as nameOf } from '@/utils/display'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  taskId: { type: [Number, String], default: null },
  projectId: { type: [Number, String], default: null },
  users: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'changed', 'open'])

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

const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const userName = (id) => nameOf(props.users, id)
const priorityLabel = (p) => TASK_PRIORITIES.find((x) => x.value === p)?.label || '—'
const nextStatuses = computed(() => TASK_TRANSITIONS[task.value.status] || [])

// 打开或在打开状态下切换 taskId（如点击子任务钻取）都要重载，且复位 Tab
watch(() => [props.modelValue, props.taskId], ([open, id]) => {
  if (open && id) {
    tab.value = 'info'
    sideTab.value = 'comment'
    reload()
  }
})

// reload 仅刷新本抽屉数据，不再 emit('changed')，避免每次打开都触发父级整表重拉
async function reload() {
  if (!props.taskId) return
  loading.value = true
  try {
    task.value = await taskApi.get(props.taskId)
    Object.assign(form, {
      title: task.value.title, assigneeId: task.value.assigneeId, priority: task.value.priority,
      stage: task.value.stage, startDate: task.value.startDate, dueDate: task.value.dueDate,
      description: task.value.description,
    })
    milestone.value = task.value.isMilestone === 1
    subtasks.value = await taskApi.subtasks(props.taskId)
  } finally {
    loading.value = false
  }
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
  gap: var(--mido-space-4);
  flex: 1;
  min-height: 0;
  margin-top: var(--mido-space-3);
}
.td__main {
  flex: 1;
  min-width: 0;
}
.td__side {
  width: var(--mido-drawer-width);
  flex: none;
  border-left: var(--mido-border-width) solid var(--el-border-color-light);
  padding-left: var(--mido-space-4);
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
