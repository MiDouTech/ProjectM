<template>
  <div class="mido-layout">
    <!-- 顶栏 TopBar（design-system §4，高度走 --mido-topbar-height）-->
    <header class="mido-topbar">
      <div class="mido-topbar__brand">
        <el-icon class="mido-topbar__toggle" @click="collapsed = !collapsed">
          <component :is="collapsed ? Expand : Fold" />
        </el-icon>
        <img v-if="logoOk" class="mido-topbar__logo" src="/logo_竖_蓝色.png"
          alt="米多 · 通用项目管理系统" @error="logoOk = false" />
        <el-icon v-else class="mido-topbar__logo-fallback"><Grid /></el-icon>
        <span v-show="!collapsed" class="mido-h2">米多项目管理</span>
      </div>
      <div class="mido-topbar__spacer" />
      <div class="mido-topbar__actions">
        <!-- 平台公告：有生效公告才显角标，点击弹出列表 -->
        <el-popover placement="bottom-end" :width="360" trigger="click">
          <template #reference>
            <el-badge :value="announcements.length" :max="99" :hidden="!announcements.length" class="mido-topbar__bell">
              <el-icon class="mido-topbar__icon" role="button" tabindex="0" aria-label="平台公告">
                <BellFilled />
              </el-icon>
            </el-badge>
          </template>
          <div class="mido-anno">
            <div class="mido-anno__title">平台公告</div>
            <el-scrollbar v-if="announcements.length" max-height="320px">
              <div v-for="a in announcements" :key="a.id" class="mido-anno__item">
                <div class="mido-anno__head">
                  <span class="mido-anno__name">{{ a.title }}</span>
                  <StatusTag :status="a.level" />
                </div>
                <div class="mido-anno__content">{{ a.content }}</div>
              </div>
            </el-scrollbar>
            <el-empty v-else description="暂无公告" :image-size="60" />
          </div>
        </el-popover>
        <el-badge :value="unread" :max="99" :hidden="!unread" class="mido-topbar__bell">
          <el-icon class="mido-topbar__icon" role="button" tabindex="0"
            :aria-label="unread ? `通知，${unread} 条未读` : '通知'"
            @click="goNotifications" @keydown.enter="goNotifications" @keydown.space.prevent="goNotifications">
            <Bell />
          </el-icon>
        </el-badge>
        <el-dropdown @command="onUserCommand">
          <el-avatar class="mido-topbar__avatar" :src="myAvatarUrl">{{ myInitial }}</el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="mido-body">
      <!-- 左侧深色主导航（design-system §4）-->
      <aside class="mido-nav" :class="{ 'mido-nav--collapsed': collapsed }">
        <el-menu
          :default-active="activeMenu"
          class="mido-nav__menu"
          :collapse="collapsed"
          router
          background-color="transparent"
        >
          <el-menu-item
            v-for="item in visibleNavItems"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区（视图容器）-->
      <main class="mido-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, BellFilled, Grid, Fold, Expand } from '@element-plus/icons-vue'
import { navItems } from '@/router'
import { useUserStore } from '@/store/user'
import { notificationApi } from '@/api/collab'
import { userApi } from '@/api/org'
import { attachmentApi } from '@/api/attachment'
import { appApi } from '@/api/app'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// 高亮顶层导航（嵌套子路由如 /admin/members 也命中 /admin）
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'workbench'))

// 顶级导航 → 功能码映射；未列出的项无对应功能码，始终显示。
// features 为空（未取到）时 hasFeature 走 fail-open 默认全显示。
const NAV_FEATURE_MAP = {
  '/goal': 'okr',
  '/doc': 'doc',
  '/report': 'report',
  '/change': 'change',
}
const visibleNavItems = computed(() =>
  navItems.filter((item) => {
    const code = NAV_FEATURE_MAP[item.path]
    return !code || userStore.hasFeature(code)
  }),
)

// 平台公告（当前生效），有则顶栏铃铛显角标
const announcements = ref([])
async function loadAnnouncements() {
  try {
    announcements.value = (await appApi.announcements()) || []
  } catch {
    announcements.value = []
  }
}

// 顶栏未读数：进入应用 / 路由切换时刷新，并定时轮询；页面不可见时暂停以省资源。
const unread = ref(0)
const POLL_MS = 30000
let pollTimer = null
async function loadUnread() {
  try {
    unread.value = await notificationApi.unreadCount()
  } catch {
    unread.value = 0
  }
}
function goNotifications() {
  router.push('/notifications')
}

// 当前登录用户头像（顶栏）：有头像取限时 URL，否则回落姓名首字
const myAvatarUrl = ref('')
const myInitial = ref('M')
async function loadMe() {
  const uid = useUserStore().userId
  if (!uid) return
  try {
    const me = await userApi.get(uid)
    myInitial.value = (me.name || me.username || 'M').charAt(0)
    myAvatarUrl.value = me.avatar ? await attachmentApi.downloadUrl(me.avatar) : ''
  } catch {
    myAvatarUrl.value = ''
  }
}
function startPoll() {
  stopPoll()
  pollTimer = setInterval(loadUnread, POLL_MS)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
// 标签页可见性：可见则刷新并轮询，隐藏则停轮询
function onVisibility() {
  if (document.hidden) {
    stopPoll()
  } else {
    loadUnread()
    startPoll()
  }
}
watch(() => route.path, loadUnread)

// 顶栏 logo：缺图时优雅回落到 Grid 图标
const logoOk = ref(true)

// 响应式导航：<1280 自动收起为图标态（design-system §9），亦可手动切换
const NARROW = 1280
const collapsed = ref(false)
function syncByWidth() {
  collapsed.value = window.innerWidth < NARROW
}
onMounted(() => {
  syncByWidth()
  window.addEventListener('resize', syncByWidth)
  document.addEventListener('visibilitychange', onVisibility)
  loadUnread()
  startPoll()
  loadMe()
  userStore.fetchFeatures()
  loadAnnouncements()
})
onUnmounted(() => {
  window.removeEventListener('resize', syncByWidth)
  document.removeEventListener('visibilitychange', onVisibility)
  stopPoll()
})

function onUserCommand(command) {
  if (command === 'logout') {
    useUserStore().clearToken()
    router.push('/login')
  }
}
</script>

<style scoped>
.mido-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mido-topbar {
  display: flex;
  align-items: center;
  height: var(--mido-topbar-height);
  padding: 0 var(--mido-space-4);
  background-color: var(--el-bg-color);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  z-index: var(--mido-z-nav);
}

.mido-topbar__brand {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  color: var(--el-color-primary);
}

.mido-topbar__toggle {
  font-size: var(--mido-font-size-h2);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

/* 顶栏 logo：限高自适应，竖版图按顶栏行高收纳 */
.mido-topbar__logo {
  height: var(--mido-space-6);
  width: auto;
  object-fit: contain;
  display: block;
}
.mido-topbar__logo-fallback {
  font-size: var(--mido-font-size-h1);
}

.mido-topbar__spacer {
  flex: 1;
}

.mido-topbar__actions {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
}

/* 通知铃铛：el-badge 默认 inline-block 基线对齐会让图标偏上，改 flex 垂直居中 */
.mido-topbar__bell {
  display: flex;
  align-items: center;
}

.mido-topbar__icon {
  display: flex;
  font-size: var(--mido-font-size-h2);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

.mido-topbar__avatar {
  width: var(--mido-space-6);
  height: var(--mido-space-6);
  background-color: var(--el-color-primary);
  cursor: pointer;
}

.mido-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.mido-nav {
  width: var(--mido-nav-width);
  background-color: var(--mido-nav-bg);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width var(--mido-duration) var(--mido-ease);
}
.mido-nav--collapsed {
  width: var(--mido-nav-width-collapsed);
}

/* 收起态：el-menu collapse 自身宽 64px，与导航容器对齐 */
.mido-nav__menu {
  border-right: none;
}
.mido-nav__menu:not(.el-menu--collapse) {
  width: var(--mido-nav-width);
}

@media (prefers-reduced-motion: reduce) {
  .mido-nav {
    transition: none;
  }
}

/* 深色导航的 Element Plus 菜单换肤（仅用 tokens）*/
.mido-nav__menu :deep(.el-menu-item) {
  color: var(--mido-nav-text);
  transition: background-color var(--mido-duration) var(--mido-ease),
    color var(--mido-duration) var(--mido-ease);
}

/* active：底色 + 3px 主色左强调条（Worktile 式，识别更利落）*/
.mido-nav__menu :deep(.el-menu-item.is-active) {
  color: var(--mido-nav-text-active);
  background-color: var(--mido-nav-active-bg);
  box-shadow: inset var(--mido-space-1) 0 0 var(--el-color-primary);
}

.mido-nav__menu :deep(.el-menu-item:hover) {
  background-color: var(--mido-nav-active-bg);
}

.mido-main {
  flex: 1;
  min-width: 0;
  padding: var(--mido-space-5);
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}

/* 平台公告弹层 */
.mido-anno__title {
  margin-bottom: var(--mido-space-3);
  font-size: var(--mido-font-size-h2);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
.mido-anno__item {
  padding: var(--mido-space-3) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-lighter);
}
.mido-anno__item:last-child {
  border-bottom: none;
}
.mido-anno__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.mido-anno__name {
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
.mido-anno__content {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
