<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">关联关系</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建关联</el-button>
    </div>
    <p class="mido-text-secondary">定义工作项类型之间的关联语义（相关/派生），如「任务 — 产生的缺陷」，支撑追溯。</p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="关系名" min-width="140">
        <template #default="{ row }">{{ row.name || '—' }}</template>
      </el-table-column>
      <el-table-column label="源类型" min-width="120">
        <template #default="{ row }">{{ row.sourceTypeName || row.sourceTypeId }}</template>
      </el-table-column>
      <el-table-column label="关系" width="100">
        <template #default="{ row }">{{ kindLabel(row.relationKind) }}</template>
      </el-table-column>
      <el-table-column label="目标类型" min-width="120">
        <template #default="{ row }">{{ row.targetTypeName || row.targetTypeId }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无关联定义，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑关联' : '新建关联'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="源类型" prop="sourceTypeId">
          <el-select v-model="form.sourceTypeId" class="full">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系" prop="relationKind">
          <el-select v-model="form.relationKind" class="full">
            <el-option v-for="k in RELATION_KINDS" :key="k.value" :label="k.label" :value="k.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标类型" prop="targetTypeId">
          <el-select v-model="form.targetTypeId" class="full">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系名"><el-input v-model="form.name" placeholder="如 产生的缺陷" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { relationDefApi, workItemTypeApi, RELATION_KINDS } from '@/api/task'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const types = ref([])
const drawer = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ sourceTypeId: null, targetTypeId: null, relationKind: 'related', name: '' })
const rules = {
  sourceTypeId: [{ required: true, message: '请选择源类型', trigger: 'change' }],
  targetTypeId: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  relationKind: [{ required: true, message: '请选择关系', trigger: 'change' }],
}
const kindLabel = (k) => RELATION_KINDS.find((x) => x.value === k)?.label || k

async function load() {
  loading.value = true
  try {
    rows.value = await relationDefApi.list()
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, { sourceTypeId: null, targetTypeId: null, relationKind: 'related', name: '' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, {
    sourceTypeId: row.sourceTypeId, targetTypeId: row.targetTypeId,
    relationKind: row.relationKind, name: row.name,
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editing.value) await relationDefApi.update(editId.value, payload)
    else await relationDefApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除该关联定义？', '提示', { type: 'warning' })
  await relationDefApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  try {
    types.value = await workItemTypeApi.list(true)
  } catch {
    types.value = []
  }
})
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
</style>
