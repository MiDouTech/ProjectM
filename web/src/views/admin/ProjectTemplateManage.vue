<template>
  <div class="mido-page ptpl">
    <div class="ptpl__bar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建模板</el-button>
    </div>
    <p class="mido-text-secondary">内置 5 套不可改/删（保种子）；自定义模板可增删改。config 为阶段/任务骨架/默认权重/审批流的 JSON。</p>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="name" label="模板名" min-width="180" />
      <el-table-column prop="category" label="类型" width="90" />
      <el-table-column prop="subCategory" label="子类" width="120">
        <template #default="{ row }">{{ row.subCategory || '—' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.isBuiltin ? '内置' : '自定义'" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.isBuiltin" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="!row.isBuiltin" link type="danger" @click="remove(row)">删除</el-button>
          <span v-if="row.isBuiltin" class="mido-text-secondary">内置只读</span>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无模板" :image-size="60" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑模板' : '新建模板'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80">
        <el-form-item label="模板名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" prop="category">
          <el-select v-model="form.category" class="full">
            <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="子类"><el-input v-model="form.subCategory" placeholder="可选（如 常规运营）" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="配置(JSON)">
          <el-input v-model="form.config" type="textarea" :rows="10" class="mido-mono"
            placeholder='{"phases":[{"name":"立项","tasks":["填写立项申请"]}],"stakeholders":[],"approvalFlow":""}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { templateApi } from '@/api/project'

const CATEGORIES = [
  { value: 'S', label: 'S 战略级' },
  { value: 'I', label: 'I 创新级' },
  { value: 'O', label: 'O 运营级' },
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', category: 'S', subCategory: '', description: '', config: '' })
const rules = {
  name: [{ required: true, message: '请输入模板名', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    rows.value = await templateApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, name: '', category: 'S', subCategory: '', description: '', config: '' })
  drawer.value = true
  formRef.value?.clearValidate?.()
}

async function openEdit(row) {
  editing.value = true
  const d = await templateApi.detail(row.id)
  Object.assign(form, {
    id: d.id, name: d.name, category: d.category, subCategory: d.subCategory || '',
    description: d.description || '', config: d.config || '',
  })
  drawer.value = true
  formRef.value?.clearValidate?.()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, category: form.category, subCategory: form.subCategory || null,
      description: form.description || null, config: form.config || null,
    }
    if (editing.value) await templateApi.update(form.id, payload)
    else await templateApi.create(payload)
    ElMessage.success('已保存')
    drawer.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除模板「${row.name}」？`, '提示', { type: 'warning' })
  await templateApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

load()
</script>

<style scoped>
.ptpl__bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.full {
  width: 100%;
}
</style>
