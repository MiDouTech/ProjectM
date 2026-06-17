<template>
  <div class="att" v-loading="loading">
    <el-upload :show-file-list="false" :http-request="doUpload" :disabled="uploading">
      <el-button type="primary" :icon="Upload" :loading="uploading">上传附件</el-button>
    </el-upload>

    <ul v-if="items.length" class="att__list">
      <li v-for="a in items" :key="a.id" class="att__item">
        <el-icon class="att__icon"><Document /></el-icon>
        <div class="att__main">
          <div class="att__name">{{ a.name }}</div>
          <div class="att__meta mido-text-secondary">
            {{ fmtSize(a.size) }}
            <template v-if="uploaderName(a.uploaderId)"> · {{ uploaderName(a.uploaderId) }}</template>
            <template v-if="a.createTime"> · {{ fmtTime(a.createTime) }}</template>
          </div>
        </div>
        <div class="att__ops">
          <el-button link type="primary" @click="download(a)">下载</el-button>
          <el-button link type="danger" @click="remove(a)">删除</el-button>
        </div>
      </li>
    </ul>
    <el-empty v-else-if="!loading" description="暂无附件" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Document } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/attachment'

const props = defineProps({
  entityType: { type: String, required: true },
  entityId: { type: [Number, String], default: null },
  // 可选：解析上传人 ID → 姓名
  userName: { type: Function, default: null },
})

const items = ref([])
const loading = ref(false)
const uploading = ref(false)

const uploaderName = (id) => (props.userName && id ? props.userName(id) : '')

function fmtSize(bytes) {
  const n = Number(bytes) || 0
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}
function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  if (!props.entityId) return
  loading.value = true
  try {
    items.value = await attachmentApi.list(props.entityType, props.entityId)
  } finally {
    loading.value = false
  }
}

// el-upload 自定义上传：走后端代理上传接口
async function doUpload(option) {
  uploading.value = true
  try {
    await attachmentApi.upload(props.entityType, props.entityId, option.file)
    ElMessage.success('上传成功')
    await load()
  } catch (e) {
    option.onError?.(e)
  } finally {
    uploading.value = false
  }
}

async function download(a) {
  const url = await attachmentApi.downloadUrl(a.id)
  if (url) window.open(url, '_blank')
}

async function remove(a) {
  await ElMessageBox.confirm(`确认删除附件「${a.name}」？`, '提示', { type: 'warning' })
  await attachmentApi.remove(a.id)
  ElMessage.success('已删除')
  load()
}

watch(() => props.entityId, () => { items.value = []; load() }, { immediate: true })
</script>

<style scoped>
.att__list {
  list-style: none;
  margin: var(--mido-space-3) 0 0;
  padding: 0;
}
.att__item {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.att__icon {
  color: var(--el-text-color-secondary);
}
.att__main {
  flex: 1;
  min-width: 0;
}
.att__name {
  font-weight: var(--mido-font-weight-bold);
  word-break: break-all;
}
.att__meta {
  font-size: var(--mido-font-size-caption);
}
.att__ops {
  flex: none;
}
</style>
