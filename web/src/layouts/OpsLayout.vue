<template>
  <div class="mido-layout">
    <!-- 顶栏 TopBar（沿用 design-system §4，token 化）-->
    <header class="mido-topbar">
      <div class="mido-topbar__brand">
        <el-icon class="mido-topbar__toggle" @click="collapsed = !collapsed">
          <component :is="collapsed ? Expand : Fold" />
        </el-icon>
        <el-icon class="mido-topbar__logo-fallback"><Platform /></el-icon>
        <span v-show="!collapsed" class="mido-h2">米多 · 平台运营后台</span>
      </div>
      <div class="mido-topbar__spacer" />
      <div class="mido-topbar__actions">
        <el-dropdown @command="onUserCommand">
          <span class="mido-topbar__account">
            <el-avatar class="mido-topbar__avatar">{{ accountInitial }}</el-avatar>
            <span class="mido-topbar__account-name">{{ accountName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="mido-body">
      <!-- 左侧深色主导航 -->
      <aside class="mido-nav" :class="{ 'mido-nav--collapsed': collapsed }">
        <el-menu
          :default-active="activeMenu"
          class="mido-nav__menu"
          :collapse="collapsed"
          router
          background-color="transparent"
        >
          <el-menu-item
            v-for="item in opsNavItems"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="mido-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Platform } from '@element-plus/icons-vue'
import { opsNavItems } from '@/router'
import { useOpsUserStore } from '@/store/opsUser'

const route = useRoute()
const router = useRouter()
const opsStore = useOpsUserStore()

// 高亮当前二级导航（/ops/tenants 等直接整段匹配）
const activeMenu = computed(() => route.path)

const accountName = computed(() => opsStore.displayName)
const accountInitial = computed(() => (accountName.value || 'M').charAt(0))

// 响应式导航：<1280 自动收起
const NARROW = 1280
const collapsed = ref(false)
function syncByWidth() {
  collapsed.value = window.innerWidth < NARROW
}

onMounted(() => {
  syncByWidth()
  window.addEventListener('resize', syncByWidth)
  // 拉取当前运营账号信息（含 perms）；失败不阻断布局
  if (!opsStore.me) {
    opsStore.fetchMe().catch(() => {})
  }
})

function onUserCommand(command) {
  if (command === 'logout') {
    opsStore.clearToken()
    router.push('/ops/login')
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

.mido-topbar__account {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  cursor: pointer;
}

.mido-topbar__account-name {
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-regular);
}

.mido-topbar__avatar {
  width: var(--mido-space-6);
  height: var(--mido-space-6);
  background-color: var(--el-color-primary);
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

.mido-nav__menu :deep(.el-menu-item) {
  color: var(--mido-nav-text);
  transition: background-color var(--mido-duration) var(--mido-ease),
    color var(--mido-duration) var(--mido-ease);
}

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
