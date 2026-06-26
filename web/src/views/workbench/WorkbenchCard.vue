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
          <el-button link :icon="Refresh" aria-label="刷新" @click="load" />
          <el-button v-if="!card.basic" link :icon="Close" aria-label="关闭卡片" @click="$emit('remove', card.id)" />
        </div>
      </div>
    </template>

    <div v-loading="loading" class="wc__body">
      <!-- 我参与的项目 / 待我验收的项目 -->
      <template v-if="card.type === 'projects' || card.type === 'pendingVerify'">
        <div v-for="p in items" :key="p.id" class="wc__row" role="button" tabindex="0"
          @click="$router.push(projectRoute(p))" @keyup.enter="$router.push(projectRoute(p))">
          <CategoryBadge :category="p.category" :show-label="false" />
          <span class="wc__row-main">{{ p.name }}</span>
          <StatusTag :status="p.status" />
        </div>
      </template>

      <!-- 我负责的任务 / 逾期任务预警 -->
      <template v-else-if="card.type === 'tasks' || card.type === 'overdueTasks'">
        <div v-for="t in items" :key="t.id" class="wc__row" role="button" tabindex="0"
          @click="$router.push(`/project/${t.projectId}/tasks`)" @keyup.enter="$router.push(`/project/${t.projectId}/tasks`)">
          <span class="wc__row-main">{{ t.title }}</span>
          <StatusTag v-if="overdue(t)" status="逾期" />
          <StatusTag :status="t.status" :color="statusColor(t.status)" />
        </div>
      </template>

      <!-- 我的待办通知 -->
      <template v-else-if="card.type === 'notifications'">
        <div class="wc__bar">
          <span class="mido-text-secondary">{{ items.length ? `${items.length} 条未读` : '' }}</span>
          <span>
            <el-button link type="primary" @click="$router.push('/notifications')">查看全部</el-button>
            <el-button v-if="items.length" link type="primary" @click="markAll">全部已读</el-button>
          </span>
        </div>
        <div v-for="n in items" :key="n.id" class="wc__row" role="button" tabindex="0"
          @click="openNotification(n)" @keyup.enter="openNotification(n)">
          <span class="wc__row-main">{{ n.title }}</span>
          <span class="mido-text-secondary">{{ fmt(n.createTime) }}</span>
          <el-button link type="primary" @click.stop="read(n)">已读</el-button>
        </div>
      </template>

      <!-- 待我审批的立项 -->
      <template v-else-if="card.type === 'approvals'">
        <div v-for="a in items" :key="a.instanceId" class="wc__row" role="button" tabindex="0"
          @click="$router.push({ path: '/approval', query: { open: a.instanceId } })"
          @keyup.enter="$router.push({ path: '/approval', query: { open: a.instanceId } })">
          <span class="wc__row-main">{{ a.title || bizLabel(a.bizType) }}</span>
          <span class="mido-text-secondary">{{ fmt(a.submittedAt) }}</span>
        </div>
      </template>

      <el-empty v-if="emptyShown" description="暂无内容" :image-size="60" />
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Rank, Refresh, Close } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import { useUserStore } from '@/store/user'
import { projectApi, approvalApi } from '@/api/project'
import { taskApi } from '@/api/task'
import { notificationApi } from '@/api/collab'
import { isTaskOverdue, formatDateTime, notificationRoute } from '@/utils/display'
import { useStatusColors } from '@/composables/useStatusColors'

const router = useRouter()
const { statusColor } = useStatusColors()

const props = defineProps({
  card: { type: Object, required: true },
})
defineEmits(['remove'])

const loading = ref(false)
const items = ref([])
const userId = useUserStore().userId

// 各卡片类型的数据加载器（统一返回数组或分页 {list}，由 load 归一）
const LOADERS = {
  projects: () => projectApi.mine(),
  // 待我验收：我参与的项目中处于「结果验收」态的（前端过滤，复用既有接口）
  pendingVerify: async () => (await projectApi.mine() || []).filter((p) => p.status === '结果验收'),
  approvals: () => approvalApi.mine(),
  tasks: () => taskApi.query({ page: 1, size: 50, assigneeId: userId }),
  // 逾期任务预警：我负责的任务中已逾期的（前端按 isTaskOverdue 过滤，复用既有接口）
  overdueTasks: async () => {
    const res = await taskApi.query({ page: 1, size: 50, assigneeId: userId })
    return (res.list || []).filter((t) => isTaskOverdue(t))
  },
  notifications: () => notificationApi.list({ page: 1, size: 50, unread: true }),
}

// 项目卡行跳转：待我验收卡直达项目验收，其余进项目列表
function projectRoute(p) {
  return props.card.type === 'pendingVerify' ? `/project/${p.id}` : '/project'
}

const count = computed(() => items.value.length)
const emptyShown = computed(() => !loading.value && !items.value.length)

const overdue = (t) => isTaskOverdue(t)
const fmt = (t) => formatDateTime(t, 5, 16)
const bizLabel = (bizType) => (bizType === 'project_init' ? '立项审批' : bizType || '审批')

async function load() {
  const loader = LOADERS[props.card.type]
  if (!loader) return
  loading.value = true
  try {
    const res = await loader()
    items.value = Array.isArray(res) ? res : (res.list || [])
  } finally {
    loading.value = false
  }
}
async function read(n) {
  await notificationApi.markRead(n.id)
  load()
}
// 点击通知：标记已读并按 bizType/link 跳转到对应详情；无法定位则仅刷新
async function openNotification(n) {
  try { await notificationApi.markRead(n.id) } catch { /* 忽略：跳转优先 */ }
  const to = notificationRoute(n)
  if (to) router.push(to)
  else load()
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
</style>
