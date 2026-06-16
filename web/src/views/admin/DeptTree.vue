<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">部门树</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate(0)">新建根部门</el-button>
    </div>

    <el-tree v-loading="loading" :data="tree" :props="treeProps" node-key="id" default-expand-all>
      <template #default="{ node, data }">
        <div class="node">
          <span>{{ node.label }}</span>
          <span class="node__actions">
            <el-button link type="primary" @click.stop="openCreate(data.id)">加子部门</el-button>
            <el-button link type="primary" @click.stop="openEdit(data)">编辑</el-button>
            <el-button link type="danger" @click.stop="remove(data)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>
    <el-empty v-if="!loading && !tree.length" description="暂无部门，点击新建根部门" />

    <el-drawer v-model="drawer" :title="editing ? '编辑部门' : '新建部门'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="部门名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上级">
          <span class="mido-text-secondary">{{ parentName }}</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deptApi } from '@/api/org'

const loading = ref(false)
const saving = ref(false)
const tree = ref([])
const treeProps = { label: 'name', children: 'children' }

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', parentId: 0 })
const rules = { name: [{ required: true, message: '请输入部门名', trigger: 'blur' }] }

const nameMap = computed(() => {
  const map = { 0: '根' }
  const walk = (nodes) => nodes?.forEach((n) => { map[n.id] = n.name; walk(n.children) })
  walk(tree.value)
  return map
})
const parentName = computed(() => nameMap.value[form.parentId] || '根')

async function load() {
  loading.value = true
  try {
    tree.value = await deptApi.tree()
  } finally {
    loading.value = false
  }
}

function openCreate(parentId) {
  editing.value = false
  Object.assign(form, { id: null, name: '', parentId })
  drawer.value = true
}
function openEdit(data) {
  editing.value = true
  Object.assign(form, { id: data.id, name: data.name, parentId: data.parentId })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) await deptApi.update(form.id, { name: form.name, parentId: form.parentId })
    else await deptApi.create({ name: form.name, parentId: form.parentId })
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(data) {
  await ElMessageBox.confirm(`确认删除部门「${data.name}」？`, '提示', { type: 'warning' })
  await deptApi.remove(data.id)
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
.node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: var(--mido-space-3);
}
.node__actions {
  display: flex;
  gap: var(--mido-space-1);
}
</style>
