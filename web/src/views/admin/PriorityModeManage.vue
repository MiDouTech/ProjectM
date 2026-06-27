<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">优先级模式</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建模式</el-button>
    </div>
    <p class="mido-text-secondary">不同业务可用不同优先级档位集（如缺陷模式/默认模式）；档位值越小优先级越高。</p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="模式名" min-width="140" />
      <el-table-column label="档位" min-width="200">
        <template #default="{ row }">
          <span class="mido-text-secondary">{{ (row.levels || []).map((l) => l.name).join(' / ') || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.builtin === 1" size="small" type="info" effect="plain">内置</el-tag>
          <span v-else class="mido-text-secondary">自定义</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="row.builtin === 1" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无优先级模式，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑优先级模式' : '新建优先级模式'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="模式名" prop="name"><el-input v-model="form.name" placeholder="如 缺陷优先级模式" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="档位">
          <div class="levels">
            <div v-for="(l, i) in form.levels" :key="i" class="level">
              <el-input v-model="l.name" placeholder="名称" class="level__name" />
              <el-select v-model="l.color" placeholder="色" class="level__color">
                <el-option v-for="c in COLOR_OPTIONS" :key="c.value" :label="c.label" :value="c.value">
                  <ColorDot :color="c.value" />{{ c.label }}
                </el-option>
              </el-select>
              <el-input-number v-model="l.levelValue" :min="1" :controls="false" class="level__val" placeholder="值" />
              <el-button link type="danger" :icon="Delete" @click="form.levels.splice(i, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="form.levels.push({ name: '', color: 'info', levelValue: form.levels.length + 1 })">添加档位</el-button>
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
import ColorDot from '@/components/ColorDot.vue'
import { priorityModeApi } from '@/api/task'

// 档位色：色块 + 中文，不暴露 token 名
const COLOR_OPTIONS = [
  { value: 'danger', label: '红' },
  { value: 'warning', label: '橙' },
  { value: 'primary', label: '蓝' },
  { value: 'info', label: '灰' },
  { value: 'success', label: '绿' },
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawer = ref(false)
const editing = ref(false)
const editId = ref(null)
const formRef = ref()
const form = reactive({ name: '', remark: '', levels: [] })
const rules = { name: [{ required: true, message: '请输入模式名', trigger: 'blur' }] }

async function load() {
  loading.value = true
  try {
    rows.value = await priorityModeApi.list()
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editing.value = false
  editId.value = null
  Object.assign(form, { name: '', remark: '', levels: [{ name: '高', color: 'danger', levelValue: 1 }] })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, remark: row.remark,
    levels: (row.levels || []).map((l) => ({ ...l })),
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, remark: form.remark,
      levels: form.levels.filter((l) => l.name && l.levelValue != null),
    }
    if (editing.value) await priorityModeApi.update(editId.value, payload)
    else await priorityModeApi.create(payload)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除优先级模式「${row.name}」？`, '提示', { type: 'warning' })
  await priorityModeApi.remove(row.id)
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
.levels {
  width: 100%;
}
.level {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.level__name {
  flex: 1;
}
.level__color {
  width: 110px;
}
.level__val {
  width: 90px;
}
</style>
