<template>
  <div class="mido-page">
    <div class="fd__bar">
      <h1 class="mido-h1">自定义字段</h1>
      <span class="mido-text-secondary">按作用域配置任务/项目的自定义字段；单选/多选需维护选项。停用后详情不再显示，存量值保留。</span>
    </div>

    <el-card shadow="never" v-loading="loading">
      <div class="fd__toolbar">
        <el-radio-group v-model="scope" @change="load">
          <el-radio-button value="task">任务字段</el-radio-button>
          <el-radio-button value="project">项目字段</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建字段</el-button>
      </div>

      <el-table :data="rows">
        <el-table-column label="显示名" min-width="140" prop="name" />
        <el-table-column label="标识" min-width="120">
          <template #default="{ row }"><span class="mido-mono mido-text-secondary">{{ row.fieldKey }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="选项" min-width="160">
          <template #default="{ row }">
            <span v-if="isOptionType(row.type)" class="mido-text-secondary">{{ (row.options || []).map((o) => o.label).join('、') || '—' }}</span>
            <span v-else class="mido-text-secondary">—</span>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70">
          <template #default="{ row }"><el-tag v-if="row.required" size="small" type="warning">必填</el-tag><span v-else class="mido-text-secondary">否</span></template>
        </el-table-column>
        <el-table-column label="排序" width="70" prop="sortNo" />
        <el-table-column label="启用" width="70">
          <template #default="{ row }"><el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无字段，点击「新建字段」添加" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="editing ? '编辑字段' : '新建字段'" width="var(--mido-dialog-width, 560px)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="显示名" prop="name"><el-input v-model="form.name" placeholder="如 客户名称" /></el-form-item>
        <el-form-item label="标识" prop="fieldKey">
          <el-input v-model="form.fieldKey" :disabled="editing" placeholder="英文标识，如 customer" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" :disabled="editing" class="fd__full" @change="onTypeChange">
            <el-option v-for="t in FIELD_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isOptionType(form.type)" label="选项">
          <div class="fd__options">
            <div v-for="(o, i) in form.options" :key="i" class="fd__option">
              <el-input v-model="o.value" placeholder="值(英文)" class="fd__opt-val" />
              <el-input v-model="o.label" placeholder="显示文案" />
              <el-button link type="danger" :icon="Delete" @click="form.options.splice(i, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="form.options.push({ value: '', label: '' })">添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="必填"><el-switch v-model="form.required" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" :controls="false" /></el-form-item>
        <el-form-item v-if="editing" label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { fieldDefApi, FIELD_TYPES } from '@/api/field'

const loading = ref(false)
const saving = ref(false)
const scope = ref('task')
const rows = ref([])
const dialog = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ fieldKey: '', name: '', type: 'text', options: [], required: false, sortNo: 0, enabled: true })
const rules = {
  name: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
  fieldKey: [{ required: true, message: '请输入字段标识', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

const typeLabel = (t) => FIELD_TYPES.find((x) => x.value === t)?.label || t
const isOptionType = (t) => t === 'select' || t === 'multi_select'

async function load() {
  loading.value = true
  try {
    rows.value = await fieldDefApi.list(scope.value, false)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(form, { fieldKey: '', name: '', type: 'text', options: [], required: false, sortNo: 0, enabled: true })
}
function onTypeChange() {
  if (isOptionType(form.type) && form.options.length === 0) {
    form.options.push({ value: '', label: '' })
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  reset()
  dialog.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, {
    fieldKey: row.fieldKey, name: row.name, type: row.type,
    options: (row.options || []).map((o) => ({ ...o })),
    required: row.required, sortNo: row.sortNo ?? 0, enabled: row.enabled,
  })
  dialog.value = true
}

function validOptions() {
  if (!isOptionType(form.type)) return null
  const opts = form.options.filter((o) => o.value && o.label)
  if (opts.length === 0) {
    ElMessage.warning('选项型字段至少需要一个有值的选项')
    return false
  }
  return opts
}

async function submit() {
  await formRef.value.validate()
  const opts = validOptions()
  if (opts === false) return
  saving.value = true
  try {
    if (editing.value) {
      await fieldDefApi.update(editId.value, {
        name: form.name, type: form.type, options: opts,
        required: form.required, sortNo: form.sortNo, enabled: form.enabled,
      })
    } else {
      await fieldDefApi.create({
        scope: scope.value, fieldKey: form.fieldKey, name: form.name, type: form.type,
        options: opts, required: form.required, sortNo: form.sortNo,
      })
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除字段「${row.name}」？存量值将保留但不再显示。`, '删除字段', { type: 'warning' })
  await fieldDefApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.fd__bar {
  display: flex;
  align-items: baseline;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
  flex-wrap: wrap;
}
.fd__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--mido-space-3);
}
.fd__full { width: 100%; }
.fd__options {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
}
.fd__option {
  display: flex;
  gap: var(--mido-space-2);
  align-items: center;
}
.fd__opt-val { max-width: 160px; }
</style>
