<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">工作项类型</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建类型</el-button>
    </div>
    <p class="mido-text-secondary">每个工作项类型 = 字段集 + 工作流（状态流转矩阵）。状态流转由矩阵驱动（取代硬编码）。</p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="类型名" min-width="180">
        <template #default="{ row }">
          <div class="wit-name">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.builtin === 1" size="small" type="info" effect="plain">内置</el-tag>
            <span class="mido-mono mido-text-secondary wit-code">{{ row.code }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="groupName" label="分组" width="120">
        <template #default="{ row }">{{ row.groupName || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openConfig(row)">配置</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="row.builtin === 1" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无工作项类型，点击新建" /></template>
    </el-table>

    <!-- 新建/编辑类型 -->
    <el-drawer v-model="drawer" :title="editing ? '编辑类型' : '新建类型'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="类型名" prop="name"><el-input v-model="form.name" placeholder="如 缺陷" /></el-form-item>
        <el-form-item label="类型标识" prop="code">
          <el-input v-model="form.code" :disabled="editing" placeholder="仅英文/数字，如 bug" />
          <span v-if="editing" class="mido-text-secondary wit-hint">系统内部使用，创建后不可更改</span>
        </el-form-item>
        <el-form-item label="分组"><el-input v-model="form.groupName" placeholder="如 IT" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 配置：字段绑定 + 流转矩阵 -->
    <el-drawer v-model="cfgDrawer" :title="`配置 · ${cfgName}`" size="640px">
      <el-tabs v-model="cfgTab">
        <el-tab-pane label="字段" name="fields">
          <p class="mido-text-secondary">勾选本类型挂载的字段（系统字段 + 自定义字段）。</p>
          <el-select v-model="fieldKeys" multiple filterable placeholder="选择字段" class="full">
            <el-option-group label="系统字段">
              <el-option v-for="f in SYSTEM_TASK_FIELDS" :key="f.value" :label="f.label" :value="f.value" />
            </el-option-group>
            <el-option-group label="自定义字段">
              <el-option v-for="f in customFields" :key="f.fieldKey" :label="f.name" :value="f.fieldKey" />
            </el-option-group>
          </el-select>
        </el-tab-pane>
        <el-tab-pane label="工作流" name="workflow">
          <p class="mido-text-secondary">勾选允许的「行状态 → 列状态」流转。</p>
          <el-table :data="statuses" border size="small" class="matrix">
            <el-table-column label="从＼到" width="100" fixed>
              <template #default="{ row }"><StatusTag :status="row.metaCategory" :label="row.name" /></template>
            </el-table-column>
            <el-table-column v-for="to in statuses" :key="to.id" :label="to.name" align="center" width="84">
              <template #default="{ row }">
                <el-checkbox
                  v-if="row.id !== to.id"
                  :model-value="hasTransition(row.id, to.id)"
                  @change="(v) => toggleTransition(row.id, to.id, v)" />
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="cfgDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { workItemTypeApi, statusApi, SYSTEM_TASK_FIELDS } from '@/api/task'
import { fieldDefApi } from '@/api/field'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ code: '', name: '', groupName: '', sort: 0, status: 'active' })
const rules = {
  name: [{ required: true, message: '请输入类型名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
}

// 配置抽屉
const cfgDrawer = ref(false)
const cfgTab = ref('fields')
const cfgId = ref(null)
const cfgName = ref('')
const fieldKeys = ref([])
const customFields = ref([])
const statuses = ref([])
const transitionSet = ref(new Set()) // "from-to"

const tkey = (f, t) => `${f}-${t}`
const hasTransition = (f, t) => transitionSet.value.has(tkey(f, t))
function toggleTransition(f, t, v) {
  const s = new Set(transitionSet.value)
  if (v) s.add(tkey(f, t))
  else s.delete(tkey(f, t))
  transitionSet.value = s
}

async function load() {
  loading.value = true
  try {
    rows.value = await workItemTypeApi.list(false)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, { code: '', name: '', groupName: '', sort: 0, status: 'active' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, { code: row.code, name: row.name, groupName: row.groupName, sort: row.sort ?? 0, status: row.status })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { code: form.code, name: form.name, groupName: form.groupName, sort: form.sort, status: form.status }
    if (editing.value) await workItemTypeApi.update(editId.value, payload)
    else await workItemTypeApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除类型「${row.name}」？`, '提示', { type: 'warning' })
  await workItemTypeApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function openConfig(row) {
  cfgId.value = row.id
  cfgName.value = row.name
  cfgTab.value = 'fields'
  const [fields, transitions, statusList, custom] = await Promise.all([
    workItemTypeApi.getFields(row.id),
    workItemTypeApi.getTransitions(row.id),
    statusApi.list(true),
    fieldDefApi.list('task', true),
  ])
  fieldKeys.value = (fields || []).map((f) => f.fieldKey)
  statuses.value = statusList || []
  customFields.value = custom || []
  transitionSet.value = new Set((transitions || []).map((t) => tkey(t.fromStatusId, t.toStatusId)))
  cfgDrawer.value = true
}
async function saveConfig() {
  saving.value = true
  try {
    const fields = fieldKeys.value.map((k, i) => ({ fieldKey: k, required: false, sort: i }))
    const transitions = [...transitionSet.value].map((k) => {
      const [f, t] = k.split('-')
      return { fromStatusId: Number(f), toStatusId: Number(t) }
    })
    await workItemTypeApi.saveFields(cfgId.value, fields)
    await workItemTypeApi.saveTransitions(cfgId.value, transitions)
    ElMessage.success('配置已保存')
    cfgDrawer.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.full {
  width: 100%;
}
.wit-name {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.wit-code {
  font-size: var(--mido-font-size-caption);
}
.wit-hint {
  margin-left: var(--mido-space-2);
}
.matrix {
  margin-top: var(--mido-space-2);
}
.muted {
  color: var(--el-text-color-secondary);
}
</style>
