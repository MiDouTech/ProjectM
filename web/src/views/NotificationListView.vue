<template>
  <div class="mido-page">
    <div class="nl__bar">
      <h1 class="mido-h1">消息通知</h1>
      <div class="nl__ops">
        <el-radio-group v-model="onlyUnread" @change="reload">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <el-button :disabled="!hasUnread" @click="markAll">全部已读</el-button>
      </div>
    </div>

    <el-card shadow="never" v-loading="loading">
      <div
        v-for="n in items"
        :key="n.id"
        class="nl__row"
        :class="{ 'is-unread': !n.isRead }"
        @click="open(n)"
      >
        <span class="nl__dot" :class="{ 'is-on': !n.isRead }" />
        <div class="nl__main">
          <div class="nl__title">{{ n.title }}</div>
          <div class="mido-text-secondary nl__content">{{ contentOf(n) }}</div>
        </div>
        <span class="mido-text-secondary nl__time">{{ fmt(n.createTime) }}</span>
        <el-button v-if="!n.isRead" link type="primary" @click.stop="read(n)">已读</el-button>
      </div>
      <el-empty v-if="!loading && !items.length" description="暂无通知" :image-size="80" />

      <div v-if="total > size" class="nl__pager">
        <el-pagination
          v-model:current-page="page"
          background
          layout="prev, pager, next, total"
          :page-size="size"
          :total="total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/collab'
import { formatDateTime, notificationContent, notificationRoute } from '@/utils/display'

const router = useRouter()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const onlyUnread = ref(false)

const hasUnread = computed(() => items.value.some((n) => !n.isRead))
const fmt = (t) => formatDateTime(t)
const contentOf = (n) => notificationContent(n)

async function load() {
  loading.value = true
  try {
    const res = await notificationApi.list({ page: page.value, size, unread: onlyUnread.value || undefined })
    items.value = res.list || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}
function reload() {
  page.value = 1
  load()
}
async function read(n) {
  await notificationApi.markRead(n.id)
  n.isRead = 1
}
async function markAll() {
  await notificationApi.markAllRead()
  load()
}
async function open(n) {
  if (!n.isRead) {
    try { await notificationApi.markRead(n.id) } catch { /* 跳转优先 */ }
    n.isRead = 1
  }
  const to = notificationRoute(n)
  if (to) router.push(to)
}

onMounted(load)
</script>

<style scoped>
.nl__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.nl__ops {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
}
.nl__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  padding: var(--mido-space-3) var(--mido-space-2);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  cursor: pointer;
  transition: background-color var(--mido-duration) var(--mido-ease);
}
.nl__row:hover {
  background: var(--el-fill-color-light);
}
.nl__row.is-unread .nl__title {
  font-weight: var(--mido-font-weight-bold);
}
.nl__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
  background: transparent;
}
.nl__dot.is-on {
  background: var(--el-color-primary);
}
.nl__main {
  flex: 1;
  min-width: 0;
}
.nl__content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nl__time {
  flex: none;
}
.nl__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
</style>
