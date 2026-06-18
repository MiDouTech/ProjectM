<template>
  <div class="mido-layout">
    <!-- 顶栏 TopBar（design-system §4，高度走 --mido-topbar-height）-->
    <header class="mido-topbar">
      <div class="mido-topbar__brand">
        <el-icon class="mido-topbar__toggle" @click="collapsed = !collapsed">
          <component :is="collapsed ? Expand : Fold" />
        </el-icon>
        <el-icon class="mido-topbar__logo"><Grid /></el-icon>
        <span v-show="!collapsed" class="mido-h2">米多项目管理</span>
      </div>
      <div class="mido-topbar__spacer" />
      <div class="mido-topbar__actions">
        <el-button type="primary" :icon="Plus" size="small">新建</el-button>
        <el-badge :value="unread" :max="99" :hidden="!unread">
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
            v-for="item in navItems"
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
import { Plus, Bell, Grid, Fold, Expand } from '@element-plus/icons-vue'
import { navItems } from '@/router'
import { useUserStore } from '@/store/user'
import { notificationApi } from '@/api/collab'
import { userApi } from '@/api/org'
import { attachmentApi } from '@/api/attachment'

const route = useRoute()
const router = useRouter()
// 高亮顶层导航（嵌套子路由如 /admin/members 也命中 /admin）
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'workbench'))

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

.mido-topbar__logo {
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

.mido-topbar__icon {
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
</style>
