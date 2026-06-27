<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">公告管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建公告</el-button>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-table v-else v-loading="loading" :data="rows" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="级别" width="100">
        <template #default="{ row }"><StatusTag :status="row.level" /></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">{{ row.publishAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="到期时间" width="180">
        <template #default="{ row }">{{ row.expireAt || '不限期' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无公告，点击新建" /></template>
    </el-table>

    <!-- 新建 / 编辑（右抽屉）-->
    <el-drawer v-model="drawer" :title="editing ? '编辑公告' : '新建公告'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="90">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" style="width: 100%">
            <el-option v-for="l in ANNOUNCEMENT_LEVEL_OPTIONS" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ANNOUNCEMENT_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker v-model="form.publishAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可空，留空=立即" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可空，留空=不限期" style="width: 100%" />
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
import ErrorState from '@/components/ErrorState.vue'
import { announcementApi, ANNOUNCEMENT_LEVEL_OPTIONS, ANNOUNCEMENT_STATUS_OPTIONS } from '@/api/ops'

const loading = ref(false)
const loadError = ref(false)
const rows = ref([])

async function load() {
  loading.value = true
  loadError.value = false
  try {
    rows.value = await announcementApi.list()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const drawer = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, title: '', content: '', level: 'info', status: 'draft', publishAt: '', expireAt: '' })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, { id: null, title: '', content: '', level: 'info', status: 'draft', publishAt: '', expireAt: '' })
}
function openCreate() {
  editing.value = false
  resetForm()
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, title: row.title, content: row.content || '',
    level: row.level, status: row.status,
    publishAt: row.publishAt || '', expireAt: row.expireAt || '',
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      title: form.title, content: form.content, level: form.level, status: form.status,
      publishAt: form.publishAt || undefined, expireAt: form.expireAt || undefined,
    }
    if (editing.value) {
      await announcementApi.update(form.id, payload)
    } else {
      await announcementApi.create(payload)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '提示', { type: 'warning' })
  await announcementApi.remove(row.id)
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
</style>
