<template>
  <div class="pub">
    <div class="pub__bar">
      <el-icon class="pub__logo"><Document /></el-icon>
      <span class="mido-h2">米多 · 文档分享</span>
    </div>
    <div class="pub__stage" v-loading="loading">
      <template v-if="ok">
        <h1 class="mido-h1 pub__title">{{ title }}</h1>
        <DocEditor :model-value="content" :editable="false" />
      </template>
      <el-result v-else-if="!loading" icon="warning" title="无法访问"
        sub-title="分享链接无效或已过期" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Document } from '@element-plus/icons-vue'
import DocEditor from '@/components/DocEditor.vue'
import { docApi } from '@/api/doc'

const route = useRoute()
const loading = ref(true)
const ok = ref(false)
const title = ref('')
const content = ref(null)

onMounted(async () => {
  try {
    const d = await docApi.publicView(route.params.token)
    title.value = d.title
    content.value = d.content ? JSON.parse(d.content) : null
    ok.value = true
  } catch {
    ok.value = false
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.pub {
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
}
.pub__bar {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  height: var(--mido-topbar-height);
  padding: 0 var(--mido-space-5);
  background-color: var(--el-bg-color);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  color: var(--el-color-primary);
}
.pub__stage {
  max-width: 860px;
  margin: 0 auto;
  padding: var(--mido-space-6) var(--mido-space-5);
}
.pub__title {
  margin-bottom: var(--mido-space-4);
}
</style>
