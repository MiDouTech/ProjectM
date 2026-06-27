<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">状态库</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建状态</el-button>
    </div>
    <p class="mido-text-secondary">业务状态可自定义，但都归约到「未开始/进行中/已完成」三元类别，保证统计口径统一。</p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="状态名" min-width="120" />
      <el-table-column label="元类别" width="120">
        <template #default="{ row }"><StatusTag :status="row.metaCategory" /></template>
      </el-table-column>
      <el-table-column prop="groupName" label="分组" width="120">
        <template #default="{ row }">{{ row.groupName || '—' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.builtin === 1" size="small" type="info" effect="plain">内置</el-tag>
          <span v-else class="mido-text-secondary">自定义</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="row.builtin === 1" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无状态，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑状态' : '新建状态'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="状态名" prop="name"><el-input v-model="form.name" placeholder="如 测试中" /></el-form-item>
        <el-form-item label="元类别" prop="metaCategory">
          <el-select v-model="form.metaCategory" class="full">
            <el-option v-for="m in META_CATEGORIES" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <el-select v-model="form.color" class="full" placeholder="选择状态色">
            <el-option v-for="c in COLOR_OPTIONS" :key="c.value" :label="c.label" :value="c.value">
              <span class="color-opt">
                <span class="color-dot" :style="{ background: `var(--el-color-${c.value})` }" />
                {{ c.label }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="分组"><el-input v-model="form.groupName" placeholder="如 缺陷" /></el-form-item>
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
import { statusApi, META_CATEGORIES } from '@/api/task'

// 状态色：中文含义 + 色块预览，不让用户面对 info/primary 等 token 名
const COLOR_OPTIONS = [
  { value: 'info', label: '灰 · 中性/未开始' },
  { value: 'primary', label: '蓝 · 进行中' },
  { value: 'warning', label: '橙 · 风险/临期' },
  { value: 'danger', label: '红 · 逾期/阻塞' },
  { value: 'success', label: '绿 · 已完成' },
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ name: '', color: 'info', metaCategory: '进行中', groupName: '', sort: 0, status: 'active' })
const rules = {
  name: [{ required: true, message: '请输入状态名', trigger: 'blur' }],
  metaCategory: [{ required: true, message: '请选择元类别', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    rows.value = await statusApi.list(false)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, { name: '', color: 'info', metaCategory: '进行中', groupName: '', sort: 0, status: 'active' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, color: row.color || 'info', metaCategory: row.metaCategory,
    groupName: row.groupName, sort: row.sort ?? 0, status: row.status,
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, color: form.color, metaCategory: form.metaCategory,
      groupName: form.groupName, sort: form.sort, status: form.status,
    }
    if (editing.value) await statusApi.update(editId.value, payload)
    else await statusApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除状态「${row.name}」？`, '提示', { type: 'warning' })
  await statusApi.remove(row.id)
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
.color-opt {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.color-dot {
  width: var(--mido-space-3);
  height: var(--mido-space-3);
  border-radius: 50%;
  flex: none;
}
</style>
