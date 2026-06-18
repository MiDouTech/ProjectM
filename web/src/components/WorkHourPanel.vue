<template>
  <div class="wh" v-loading="loading">
    <!-- 汇总（任务级，含子任务；口径：进度=实际/预估，预估0→0） -->
    <div class="wh__caption mido-text-secondary">汇总含子任务工时；下方记录表仅为当前任务</div>
    <div class="wh__sum">
      <div class="wh__stat">
        <span class="wh__stat-label mido-text-secondary">预估</span>
        <span class="mido-mono">{{ fmtHours(summary.estHours) }}h</span>
      </div>
      <div class="wh__stat">
        <span class="wh__stat-label mido-text-secondary">实际</span>
        <span class="mido-mono">{{ fmtHours(summary.actualHours) }}h</span>
      </div>
      <div class="wh__stat">
        <span class="wh__stat-label mido-text-secondary">剩余</span>
        <span class="mido-mono">{{ fmtHours(summary.remainingHours) }}h</span>
      </div>
      <div class="wh__stat wh__stat--progress">
        <span class="wh__stat-label mido-text-secondary">进度</span>
        <el-progress :percentage="progressBar" :status="overrun ? 'exception' : undefined"
          :format="() => `${fmtHours(summary.progress)}%`" />
      </div>
    </div>

    <!-- 登记/修改表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" :inline="true" class="wh__form">
      <el-form-item label="类型" prop="kind">
        <el-radio-group v-model="form.kind" :disabled="!!editingId">
          <el-radio v-for="k in WORKHOUR_KINDS" :key="k.value" :value="k.value">{{ k.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="类别" prop="category">
        <el-select v-model="form.category" placeholder="类别">
          <el-option v-for="c in WORKHOUR_CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期" prop="workDate">
        <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" />
      </el-form-item>
      <el-form-item label="工时" prop="hours">
        <el-input-number v-model="form.hours" :min="0.5" :step="0.5" :precision="2" :controls="false" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" placeholder="可选" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">{{ editingId ? '保存' : '登记' }}</el-button>
        <el-button v-if="editingId" @click="resetForm">取消</el-button>
      </el-form-item>
    </el-form>

    <!-- 记录表 -->
    <el-table :data="records" size="small">
      <el-table-column label="类型" width="70">
        <template #default="{ row }">{{ kindLabel(row.kind) }}</template>
      </el-table-column>
      <el-table-column prop="category" label="类别" width="80" sortable />
      <el-table-column prop="workDate" label="日期" width="120" sortable />
      <el-table-column label="工时" width="90" align="right" sortable :sort-by="(row) => row.hours">
        <template #default="{ row }"><span class="mido-mono">{{ fmtHours(row.hours) }}</span></template>
      </el-table-column>
      <el-table-column label="人员" width="110">
        <template #default="{ row }">{{ uName(row.userId) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="70">
        <template #default="{ row }">
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无工时记录" :image-size="50" /></template>
    </el-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { workHourApi, WORKHOUR_KINDS, WORKHOUR_CATEGORIES } from '@/api/task'

const props = defineProps({
  taskId: { type: [Number, String], default: null },
  // 可选：解析人员 ID → 姓名
  userName: { type: Function, default: null },
})

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const summary = ref({ estHours: 0, actualHours: 0, remainingHours: 0, progress: 0 })
const editingId = ref(null)
const formRef = ref()
const form = reactive({ kind: 'actual', category: '研发', workDate: null, hours: null, remark: '' })
const rules = {
  kind: [{ required: true, message: '请选择类型', trigger: 'change' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  hours: [{ required: true, message: '请输入工时', trigger: 'blur' }],
}

const kindLabel = (k) => WORKHOUR_KINDS.find((x) => x.value === k)?.label || k
const uName = (id) => (props.userName ? props.userName(id) : (id ? `用户#${id}` : '—'))
const fmtHours = (v) => Number(v || 0).toFixed(2).replace(/\.00$/, '')
// 进度条填充取 0-100（不四舍五入避免 99.5→满格）；文案保留后端 1 位百分比；超 100% 标记超工
const progressBar = computed(() => Math.min(100, Math.max(0, Number(summary.value.progress || 0))))
const overrun = computed(() => Number(summary.value.progress || 0) > 100)

async function load() {
  if (!props.taskId) return
  loading.value = true
  try {
    const [sum, list] = await Promise.all([
      workHourApi.taskSummary(props.taskId),
      workHourApi.list(props.taskId),
    ])
    summary.value = sum
    records.value = list
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { kind: 'actual', category: '研发', workDate: null, hours: null, remark: '' })
  formRef.value?.clearValidate()
}
function edit(row) {
  editingId.value = row.id
  Object.assign(form, {
    kind: row.kind, category: row.category, workDate: row.workDate,
    hours: Number(row.hours), remark: row.remark || '',
  })
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await workHourApi.update(editingId.value, {
        category: form.category, workDate: form.workDate, hours: form.hours, remark: form.remark,
      })
    } else {
      await workHourApi.log({
        taskId: props.taskId, kind: form.kind, category: form.category,
        workDate: form.workDate, hours: form.hours, remark: form.remark,
      })
    }
    ElMessage.success(editingId.value ? '已保存' : '已登记')
    resetForm()
    load()
  } finally {
    saving.value = false
  }
}

watch(() => props.taskId, () => { resetForm(); load() }, { immediate: true })
</script>

<style scoped>
.wh__caption {
  font-size: var(--mido-font-size-caption);
  margin-bottom: var(--mido-space-2);
}
.wh__sum {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-5);
  padding: var(--mido-space-3);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  margin-bottom: var(--mido-space-4);
}
.wh__stat {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.wh__stat--progress {
  flex: 1;
}
.wh__stat-label {
  font-size: var(--mido-font-size-caption);
}
.wh__form {
  margin-bottom: var(--mido-space-3);
}
</style>
