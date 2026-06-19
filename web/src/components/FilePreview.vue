<template>
  <div class="file-preview">
    <div class="file-preview__bar">
      <el-icon><Document /></el-icon>
      <span class="file-preview__name">{{ name }}</span>
      <el-button link type="primary" :icon="Download" @click="openRaw">下载</el-button>
    </div>
    <div class="file-preview__body">
      <img v-if="kind === 'image'" :src="url" :alt="name" class="file-preview__img" />
      <iframe v-else-if="kind === 'pdf'" :src="url" class="file-preview__frame" :title="name" />
      <el-empty v-else :image-size="80"
        :description="`暂不支持在线预览 ${ext || '该格式'}，请下载查看`" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'

const props = defineProps({
  url: { type: String, default: '' },
  name: { type: String, default: '' },
})

const ext = computed(() => (props.name.split('.').pop() || '').toLowerCase())
const kind = computed(() => {
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext.value)) return 'image'
  if (ext.value === 'pdf') return 'pdf'
  return 'other'
})
function openRaw() {
  if (props.url) window.open(props.url, '_blank')
}
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
</style>
