<template>
  <div class="mido-page">
    <div class="nl__bar">
      <h1 class="mido-h1">消息中心</h1>
      <div class="nl__ops">
        <el-radio-group v-model="onlyUnread" @change="reload">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <el-button :disabled="!hasUnread" @click="markAll">全部已读</el-button>
      </div>
    </div>

    <!-- 平台公告（仅第一页展示，与系统消息统一在消息中心）-->
    <el-card v-if="page === 1 && displayedAnnos.length" shadow="never" class="nl__section">
      <div class="nl__section-title">平台公告</div>
      <div
        v-for="a in displayedAnnos"
        :key="`anno-${a.id}`"
        class="nl__row"
        :class="{ 'is-unread': isAnnoUnread(a) }"
        role="button"
        tabindex="0"
        @click="openAnno(a)"
        @keyup.enter="openAnno(a)"
      >
        <span class="nl__dot" :class="{ 'is-on': isAnnoUnread(a) }" />
        <div class="nl__main">
          <div class="nl__title">
            <el-tag size="small" type="warning" disable-transitions class="nl__src">公告</el-tag>
            <StatusTag :status="a.level" />
            <span>{{ a.title }}</span>
          </div>
          <div class="mido-text-secondary nl__content">{{ a.content }}</div>
        </div>
        <span class="mido-text-secondary nl__time">{{ fmt(a.publishAt || a.createTime) }}</span>
      </div>
    </el-card>

    <!-- 系统消息 -->
    <el-card shadow="never" class="nl__section" v-loading="loading">
      <div class="nl__section-title">系统消息</div>
      <div
        v-for="n in items"
        :key="n.id"
        class="nl__row"
        :class="{ 'is-unread': !n.isRead }"
        role="button"
        tabindex="0"
        @click="open(n)"
        @keyup.enter="open(n)"
      >
        <span class="nl__dot" :class="{ 'is-on': !n.isRead }" />
        <div class="nl__main">
          <div class="nl__title">
            <el-tag size="small" type="info" disable-transitions class="nl__src">系统</el-tag>
            <span>{{ n.title }}</span>
          </div>
          <div class="mido-text-secondary nl__content">{{ contentOf(n) }}</div>
        </div>
        <span class="mido-text-secondary nl__time">{{ fmt(n.createTime) }}</span>
        <el-button v-if="!n.isRead" link type="primary" @click.stop="read(n)">已读</el-button>
      </div>
      <el-empty v-if="!loading && !items.length" description="暂无系统消息" :image-size="80" />

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

    <!-- 消息详情：统一右抽屉（公告 / 无跳转目标的系统消息）-->
    <el-drawer v-model="drawerOpen" :title="detail.title" size="420px" direction="rtl">
      <div class="nl__detail">
        <div class="nl__detail-meta">
          <StatusTag v-if="detail.level" :status="detail.level" />
          <span class="mido-text-secondary">{{ fmt(detail.time) }}</span>
        </div>
        <div class="nl__detail-content">{{ detail.content || '（无正文）' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/collab'
import { appApi } from '@/api/app'
import { useUserStore } from '@/store/user'
import { formatDateTime, notificationContent, notificationRoute } from '@/utils/display'
import { seenIds, markSeen } from '@/utils/announcements'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const onlyUnread = ref(false)

// 平台公告（当前生效）+ 本地已看态
const annos = ref([])
const seenSet = ref(seenIds(userStore.userId))
const isAnnoUnread = (a) => !seenSet.value.has(a.id)
const displayedAnnos = computed(() =>
  onlyUnread.value ? annos.value.filter(isAnnoUnread) : annos.value,
)

// 详情抽屉
const drawerOpen = ref(false)
const detail = reactive({ title: '', level: '', content: '', time: '' })

const hasUnread = computed(() => items.value.some((n) => !n.isRead) || annos.value.some(isAnnoUnread))
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
async function loadAnnos() {
  try {
    annos.value = (await appApi.announcements()) || []
  } catch {
    annos.value = []
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
  // 系统消息走后端标记；平台公告走本地已看
  await notificationApi.markAllRead()
  annos.value.forEach((a) => markSeen(userStore.userId, a.id))
  seenSet.value = seenIds(userStore.userId)
  load()
}
async function open(n) {
  if (!n.isRead) {
    try { await notificationApi.markRead(n.id) } catch { /* 跳转优先 */ }
    n.isRead = 1
  }
  const to = notificationRoute(n)
  if (to) {
    router.push(to)
  } else {
    // 无业务跳转目标的系统消息：右抽屉看正文
    showDetail({ title: n.title, content: contentOf(n), time: n.createTime })
  }
}
function openAnno(a) {
  markSeen(userStore.userId, a.id)
  seenSet.value = seenIds(userStore.userId)
  showDetail({ title: a.title, level: a.level, content: a.content, time: a.publishAt || a.createTime })
}
function showDetail({ title, level = '', content, time }) {
  detail.title = title
  detail.level = level
  detail.content = content
  detail.time = time
  drawerOpen.value = true
}

onMounted(() => {
  load()
  loadAnnos()
})
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
.nl__section {
  margin-bottom: var(--mido-space-4);
}
.nl__section-title {
  margin-bottom: var(--mido-space-2);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
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
.nl__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.nl__src {
  flex: none;
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
.nl__detail-meta {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
}
.nl__detail-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-regular);
  line-height: var(--mido-line-height, 1.6);
}
</style>
