<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">项目角色</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建角色</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="角色名" min-width="180">
        <template #default="{ row }">
          <div class="pr-name">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.builtin === 1" size="small" type="info" effect="plain">内置</el-tag>
            <span class="mido-mono mido-text-secondary pr-code">{{ row.code }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <RowActions>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.builtin === 1" @click="remove(row)">删除</el-button>
          </RowActions>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无项目角色，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑项目角色' : '新建项目角色'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" placeholder="如 开发/测试" /></el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="form.code" :disabled="editing" placeholder="仅英文/数字，如 dev" />
          <span v-if="editing" class="mido-text-secondary pr-hint">系统内部使用，创建后不可更改</span>
        </el-form-item>
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
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import RowActions from '@/components/RowActions.vue'
import { projectRoleApi } from '@/api/project'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', code: '', sort: 0, status: 'active' })
const rules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    rows.value = await projectRoleApi.list(false)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, name: '', code: '', sort: 0, status: 'active' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, { id: row.id, name: row.name, code: row.code, sort: row.sort, status: row.status })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { code: form.code, name: form.name, sort: form.sort, status: form.status }
    if (editing.value) await projectRoleApi.update(form.id, payload)
    else await projectRoleApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除项目角色「${row.name}」？`, '提示', { type: 'warning' })
  await projectRoleApi.remove(row.id)
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
  margin-bottom: var(--mido-space-4);
}
.full {
  width: 100%;
}
.pr-name {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.pr-code {
  font-size: var(--mido-font-size-caption);
}
.pr-hint {
  margin-left: var(--mido-space-2);
}
</style>
