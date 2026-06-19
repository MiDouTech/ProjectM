<template>
  <div class="file-preview">
    <div class="file-preview__bar">
      <el-icon><Document /></el-icon>
      <span class="file-preview__name">{{ name }}</span>
      <el-button link type="primary" :icon="Download" @click="openRaw">下载</el-button>
    </div>
    <div class="file-preview__body" v-loading="rendering">
      <img v-if="kind === 'image'" :src="url" :alt="name" class="file-preview__img" />
      <iframe v-else-if="kind === 'pdf'" :src="url" class="file-preview__frame" :title="name" />
      <!-- docx 渲染容器 -->
      <div v-show="kind === 'docx'" ref="docxBox" class="file-preview__doc"></div>
      <!-- xlsx 渲染为表格 -->
      <div v-show="kind === 'xlsx'" class="file-preview__sheet" v-html="sheetHtml"></div>
      <el-empty v-if="kind === 'other'" :image-size="80"
        :description="`暂不支持在线预览 ${ext || '该格式'}，请下载查看`" />
      <el-empty v-else-if="failed" :image-size="80" description="预览失败，请下载查看" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'

const props = defineProps({
  url: { type: String, default: '' },
  name: { type: String, default: '' },
})

const docxBox = ref(null)
const sheetHtml = ref('')
const rendering = ref(false)
const failed = ref(false)

const ext = computed(() => (props.name.split('.').pop() || '').toLowerCase())
const kind = computed(() => {
  const e = ext.value
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(e)) return 'image'
  if (e === 'pdf') return 'pdf'
  if (e === 'docx') return 'docx'
  if (e === 'xlsx' || e === 'xls') return 'xlsx'
  return 'other'
})
function openRaw() {
  if (props.url) window.open(props.url, '_blank')
}

// Office：拉取文件后客户端渲染（私有文件不出域）
async function renderOffice() {
  failed.value = false
  sheetHtml.value = ''
  if (!props.url || (kind.value !== 'docx' && kind.value !== 'xlsx')) return
  rendering.value = true
  try {
    const buf = await (await fetch(props.url)).arrayBuffer()
    if (kind.value === 'docx') {
      const { renderAsync } = await import('docx-preview')
      await nextTick()
      docxBox.value.innerHTML = ''
      await renderAsync(buf, docxBox.value)
    } else {
      const XLSX = await import('xlsx')
      const wb = XLSX.read(buf, { type: 'array' })
      sheetHtml.value = wb.SheetNames
        .map((n) => `<h4 class="file-preview__sheet-title">${n}</h4>` + XLSX.utils.sheet_to_html(wb.Sheets[n]))
        .join('')
    }
  } catch {
    failed.value = true
  } finally {
    rendering.value = false
  }
}

watch(() => props.url, renderOffice, { immediate: true })
</script>

<style scoped>
.file-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 60vh;
}
.file-preview__bar {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding-bottom: var(--mido-space-3);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  margin-bottom: var(--mido-space-3);
}
.file-preview__name {
  flex: 1;
  font-weight: var(--mido-font-weight-bold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-preview__body {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
}
.file-preview__img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
.file-preview__frame {
  width: 100%;
  height: 70vh;
  border: none;
}
.file-preview__doc,
.file-preview__sheet {
  width: 100%;
  align-self: flex-start;
  overflow: auto;
}
.file-preview__sheet :deep(table) {
  border-collapse: collapse;
  font-size: var(--mido-font-size-secondary);
}
.file-preview__sheet :deep(td),
.file-preview__sheet :deep(th) {
  border: var(--mido-border-width) solid var(--el-border-color-light);
  padding: var(--mido-space-1) var(--mido-space-2);
}
.file-preview__sheet :deep(.file-preview__sheet-title) {
  margin: var(--mido-space-3) 0 var(--mido-space-2);
}
</style>
