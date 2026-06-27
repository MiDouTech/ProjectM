<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">数据源（选项集）</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建数据源</el-button>
    </div>
    <p class="mido-text-secondary">可复用的下拉选项集；下拉/多选字段可引用数据源，集中维护选项。</p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="groupName" label="分组" width="120">
        <template #default="{ row }">{{ row.groupName || '—' }}</template>
      </el-table-column>
      <el-table-column label="选项" min-width="200">
        <template #default="{ row }">
          <span class="mido-text-secondary">{{ (row.options || []).map((o) => o.label).join('、') || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无数据源，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑数据源' : '新建数据源'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="如 缺陷类型" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="form.groupName" placeholder="如 IT" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="选项">
          <div class="opts">
            <div class="opt opt--head mido-text-secondary">
              <span class="opt__v">选项值（存储用）</span>
              <span class="opt__l">显示文案（给用户看）</span>
              <span class="opt__del" />
            </div>
            <div v-for="(o, i) in form.options" :key="i" class="opt">
              <el-input v-model="o.value" placeholder="如 bug" class="opt__v" />
              <el-input v-model="o.label" placeholder="如 缺陷" class="opt__l" />
              <el-button link type="danger" :icon="Delete" class="opt__del" @click="form.options.splice(i, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="form.options.push({ value: '', label: '' })">添加选项</el-button>
          </div>
        </el-form-item>
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
import { Plus, Delete } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { dataSourceApi } from '@/api/field'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ name: '', groupName: '', remark: '', status: 'active', options: [] })
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

async function load() {
  loading.value = true
  try {
    rows.value = await dataSourceApi.list(false)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, { name: '', groupName: '', remark: '', status: 'active', options: [{ value: '', label: '' }] })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, groupName: row.groupName, remark: row.remark, status: row.status,
    options: (row.options || []).map((o) => ({ ...o })),
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, groupName: form.groupName, remark: form.remark, status: form.status,
      options: form.options.filter((o) => o.value && o.label),
    }
    if (editing.value) await dataSourceApi.update(editId.value, payload)
    else await dataSourceApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除数据源「${row.name}」？引用它的字段将回落为空选项。`, '提示', { type: 'warning' })
  await dataSourceApi.remove(row.id)
  ElMessage.success('已删除')
  load()
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
.opts {
  width: 100%;
}
.opt {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.opt--head {
  font-size: var(--mido-font-size-caption);
  margin-bottom: var(--mido-space-1);
}
.opt__v {
  width: 120px;
}
.opt__l {
  flex: 1;
}
.opt__del {
  width: var(--mido-space-6);
  flex: none;
}
</style>
