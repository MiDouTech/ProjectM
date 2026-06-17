<template>
  <div class="pf" v-loading="loading">
    <div class="pf__bar">
      <span class="mido-text-secondary">共 {{ files.length }} 个文件（含任务/费用附件）</span>
      <el-upload :show-file-list="false" :http-request="customUpload" :disabled="uploading">
        <el-button type="primary" :icon="UploadFilled" :loading="uploading">上传项目文档</el-button>
      </el-upload>
    </div>

    <el-table :data="files" size="small">
      <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
        <template #default="{ row }"><el-icon><Document /></el-icon> {{ row.name }}</template>
      </el-table-column>
      <el-table-column label="来源" width="90">
        <template #default="{ row }">{{ sourceLabel(row.entityType) }}</template>
      </el-table-column>
      <el-table-column label="大小" width="100" align="right">
        <template #default="{ row }"><span class="mido-mono">{{ fmtSize(row.size) }}</span></template>
      </el-table-column>
      <el-table-column label="上传人" width="110">
        <template #default="{ row }">{{ uName(row.createBy) }}</template>
      </el-table-column>
      <el-table-column label="上传时间" width="160" prop="createTime" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="download(row)">下载</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无项目文件" :image-size="60" /></template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/attachment'

const props = defineProps({
  projectId: { type: [Number, String], default: null },
  userName: { type: Function, default: null },
})

const loading = ref(false)
const uploading = ref(false)
const files = ref([])

const SOURCE = { project: '项目文档', task: '任务', cost: '费用' }
const sourceLabel = (t) => SOURCE[t] || t
const uName = (id) => (props.userName ? props.userName(id) : (id ? `用户#${id}` : '—'))
function fmtSize(bytes) {
  const b = Number(bytes || 0)
  if (b < 1024) return `${b} B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
  return `${(b / 1024 / 1024).toFixed(1)} MB`
}

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    files.value = await attachmentApi.listByProject(props.projectId)
  } finally {
    loading.value = false
  }
}

// 预签名直传：登记 → 直接 PUT 到对象存储（不经后端代理，不走 /api 拦截器以免污染签名）
async function customUpload({ file }) {
  uploading.value = true
  try {
    const ticket = await attachmentApi.register({
      entityType: 'project',
      entityId: props.projectId,
      name: file.name,
      size: file.size,
      contentType: file.type || 'application/octet-stream',
    })
    const resp = await fetch(ticket.uploadUrl, { method: 'PUT', body: file })
    if (!resp.ok) throw new Error(`PUT ${resp.status}`)
    ElMessage.success('上传成功')
    load()
  } catch (e) {
    ElMessage.error('上传失败：' + (e.message || ''))
  } finally {
    uploading.value = false
  }
}

async function download(row) {
  const url = await attachmentApi.downloadUrl(row.id)
  window.open(url, '_blank')
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除文件「${row.name}」?`, '提示', { type: 'warning' })
  await attachmentApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

watch(() => props.projectId, load, { immediate: true })
</script>

<style scoped>
.pf__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
</style>
