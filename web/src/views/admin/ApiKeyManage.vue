<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">开放平台 API Key</h2>
      <div class="bar__right">
        <el-button type="primary" :icon="Plus" @click="openCreate">创建 API Key</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="名称" sortable />
      <el-table-column label="前缀" width="180">
        <template #default="{ row }"><span class="mido-mono">{{ row.keyPrefix }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="最近使用" width="180">
        <template #default="{ row }">{{ row.lastUsedAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="到期" width="180">
        <template #default="{ row }">{{ row.expireAt || '长期有效' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" sortable />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'active'" link type="primary" @click="revoke(row)">停用</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无 API Key，点击创建" /></template>
    </el-table>

    <!-- 创建（右抽屉） -->
    <el-drawer v-model="drawer" title="创建 API Key" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="用于区分用途，如：报表同步" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="form.expireAt" type="datetime" placeholder="留空表示长期有效"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-drawer>

    <!-- 创建成功：明文展示（仅一次） -->
    <el-dialog v-model="secretDialog" title="API Key 创建成功" width="var(--mido-login-card-width)"
      :close-on-click-modal="false" @closed="onSecretClosed">
      <el-alert type="warning" :closable="false" show-icon
        title="此密钥仅展示一次，请立即复制保存。关闭后将无法再次查看。" style="margin-bottom: var(--mido-space-3)" />
      <el-input v-model="secret" readonly class="mido-mono">
        <template #append>
          <el-button :icon="CopyDocument" @click="copySecret">复制</el-button>
        </template>
      </el-input>
      <template #footer>
        <el-button type="primary" @click="secretDialog = false">我已保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CopyDocument } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { apiKeyApi } from '@/api/org'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])

const drawer = ref(false)
const formRef = ref()
const form = reactive({ name: '', expireAt: null })
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

const secretDialog = ref(false)
const secret = ref('')

async function load() {
  loading.value = true
  try {
    rows.value = (await apiKeyApi.list()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { name: '', expireAt: null })
  drawer.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await apiKeyApi.create({ name: form.name, expireAt: form.expireAt || undefined })
    drawer.value = false
    secret.value = res.apiKey
    secretDialog.value = true
  } finally {
    saving.value = false
  }
}

// dialog 关闭后再刷新列表（明文已展示完毕）
function onSecretClosed() {
  secret.value = ''
  load()
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(secret.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}

async function revoke(row) {
  await ElMessageBox.confirm(`确认停用 API Key「${row.name}」？停用后将无法继续调用。`, '提示', { type: 'warning' })
  await apiKeyApi.revoke(row.id)
  ElMessage.success('已停用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除 API Key「${row.name}」？此操作不可恢复。`, '提示', { type: 'warning' })
  await apiKeyApi.remove(row.id)
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
.bar__right {
  display: flex;
  gap: var(--mido-space-2);
}
</style>
