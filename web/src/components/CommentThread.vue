<template>
  <!-- 评论列表 + 发表（design-system §7-B 右活动栏）。entityType: task/project/goal。 -->
  <div class="ct" v-loading="loading">
    <div class="ct__editor">
      <el-input v-model="content" type="textarea" :rows="2" placeholder="写评论，可 @ 项目成员" />
      <div class="ct__editor-bar">
        <el-select v-model="mention" multiple collapse-tags filterable placeholder="@ 提醒" class="ct__mention">
          <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
        <el-button type="primary" :loading="saving" :disabled="!content.trim()" @click="submit">发表</el-button>
      </div>
    </div>

    <div v-for="c in comments" :key="c.id" class="ct__item">
      <div class="ct__meta">
        <span class="ct__author">{{ userName(c.userId) }}</span>
        <span class="mido-text-secondary">{{ fmt(c.createTime) }}</span>
      </div>
      <div class="ct__content">{{ c.content }}</div>
      <div v-if="c.mention?.length" class="mido-text-secondary">@ {{ c.mention.map(userName).join('、') }}</div>
    </div>
    <el-empty v-if="!loading && !comments.length" description="暂无评论" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { commentApi } from '@/api/collab'
import { userName as nameOf, formatDateTime } from '@/utils/display'

const props = defineProps({
  entityType: { type: String, required: true },
  entityId: { type: [Number, String], required: true },
  users: { type: Array, default: () => [] },
})

const loading = ref(false)
const saving = ref(false)
const comments = ref([])
const content = ref('')
const mention = ref([])

const userName = (id) => nameOf(props.users, id)
const fmt = (t) => formatDateTime(t)

async function load() {
  if (!props.entityId) return
  loading.value = true
  try {
    comments.value = await commentApi.list(props.entityType, props.entityId)
  } finally {
    loading.value = false
  }
}
async function submit() {
  saving.value = true
  try {
    await commentApi.create({
      entityType: props.entityType, entityId: props.entityId,
      content: content.value, mention: mention.value,
    })
    ElMessage.success('已发表')
    content.value = ''
    mention.value = []
    load()
  } finally {
    saving.value = false
  }
}

watch(() => [props.entityType, props.entityId], load, { immediate: true })
</script>

<style scoped>
.ct__editor {
  margin-bottom: var(--mido-space-4);
}
.ct__editor-bar {
  display: flex;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-2);
}
.ct__mention {
  flex: 1;
}
.ct__item {
  padding: var(--mido-space-3) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.ct__meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--mido-space-1);
}
.ct__author {
  font-weight: var(--mido-font-weight-bold);
}
</style>
