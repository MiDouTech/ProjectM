<template>
  <el-card shadow="never" class="wc">
    <template #header>
      <div class="wc__head">
        <div class="wc__title">
          <el-icon class="wc__drag"><Rank /></el-icon>
          <span class="mido-h2">{{ card.title }}</span>
          <el-badge v-if="count" :value="count" :max="99" type="primary" />
        </div>
        <div class="wc__ops">
          <el-button link :icon="Refresh" @click="load" />
          <el-button link :icon="Close" @click="$emit('remove', card.id)" />
        </div>
      </div>
    </template>

    <div v-loading="loading" class="wc__body">
      <!-- 我负责的项目 -->
      <template v-if="card.type === 'projects'">
        <div v-for="p in items" :key="p.id" class="wc__row" @click="$router.push('/project')">
          <CategoryBadge :category="p.category" :show-label="false" />
          <span class="wc__row-main">{{ p.name }}</span>
          <StatusTag :status="p.status" />
        </div>
      </template>

      <!-- 我负责的任务 -->
      <template v-else-if="card.type === 'tasks'">
        <div v-for="t in items" :key="t.id" class="wc__row" @click="$router.push(`/project/${t.projectId}/tasks`)">
          <span class="wc__row-main">{{ t.title }}</span>
          <span v-if="overdue(t)" class="wc__overdue">逾期</span>
          <StatusTag :status="t.status" />
        </div>
      </template>

      <!-- 我的待办通知 -->
      <template v-else-if="card.type === 'notifications'">
        <div class="wc__bar">
          <span class="mido-text-secondary">{{ items.length ? `${items.length} 条未读` : '' }}</span>
          <el-button v-if="items.length" link type="primary" @click="markAll">全部已读</el-button>
        </div>
        <div v-for="n in items" :key="n.id" class="wc__row">
          <span class="wc__row-main">{{ n.title }}</span>
          <span class="mido-text-secondary">{{ fmt(n.createTime) }}</span>
          <el-button link type="primary" @click="read(n)">已读</el-button>
        </div>
      </template>

      <!-- 待我审批的立项（待办接口就绪前为入口占位） -->
      <template v-else-if="card.type === 'approvals'">
        <el-empty :image-size="60"
          description="待我审批待办列表待后端接口就绪后接入；当前可前往审批页按实例处理">
          <el-button type="primary" @click="$router.push('/approval')">前往审批</el-button>
        </el-empty>
      </template>

      <el-empty v-if="emptyShown" description="暂无内容" :image-size="60" />
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Rank, Refresh, Close } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import { useUserStore } from '@/store/user'
import { projectApi } from '@/api/project'
import { taskApi } from '@/api/task'
import { notificationApi } from '@/api/collab'

const props = defineProps({
  card: { type: Object, required: true },
})
defineEmits(['remove'])

const loading = ref(false)
const items = ref([])
const userId = useUserStore().userId

const count = computed(() => (props.card.type === 'approvals' ? 0 : items.value.length))
const emptyShown = computed(() =>
  !loading.value && !items.value.length && !['approvals'].includes(props.card.type))

const today = new Date().toISOString().slice(0, 10)
const overdue = (t) => t.dueDate && t.dueDate < today && t.status !== '已完成' && t.status !== '已验收'
const fmt = (t) => (t ? String(t).replace('T', ' ').slice(5, 16) : '')

async function load() {
  loading.value = true
  try {
    if (props.card.type === 'projects') {
      const res = await projectApi.query({ page: 1, size: 50, leaderId: userId })
      items.value = res.list || []
    } else if (props.card.type === 'tasks') {
      const res = await taskApi.query({ page: 1, size: 50, assigneeId: userId })
      items.value = res.list || []
    } else if (props.card.type === 'notifications') {
      const res = await notificationApi.list({ page: 1, size: 50, unread: true })
      items.value = res.list || []
    }
  } finally {
    loading.value = false
  }
}
async function read(n) {
  await notificationApi.markRead(n.id)
  load()
}
async function markAll() {
  await notificationApi.markAllRead()
  load()
}

onMounted(load)
</script>

<style scoped>
.wc__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wc__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.wc__drag {
  cursor: grab;
  color: var(--el-text-color-secondary);
}
.wc__body {
  max-height: calc(var(--mido-drawer-width) * 0.6);
  overflow-y: auto;
}
.wc__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.wc__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  cursor: pointer;
}
.wc__row-main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wc__overdue {
  color: var(--el-color-danger);
  font-size: var(--mido-font-size-caption);
}
</style>
