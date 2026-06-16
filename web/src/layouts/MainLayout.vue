<template>
  <div class="mido-layout">
    <!-- 顶栏 TopBar（design-system §4，高度走 --mido-topbar-height）-->
    <header class="mido-topbar">
      <div class="mido-topbar__brand">
        <el-icon class="mido-topbar__logo"><Grid /></el-icon>
        <span class="mido-h2">米多项目管理</span>
      </div>
      <div class="mido-topbar__spacer" />
      <div class="mido-topbar__actions">
        <el-button type="primary" :icon="Plus" size="small">新建</el-button>
        <el-badge :is-dot="true">
          <el-icon class="mido-topbar__icon"><Bell /></el-icon>
        </el-badge>
        <el-avatar class="mido-topbar__avatar">M</el-avatar>
      </div>
    </header>

    <div class="mido-body">
      <!-- 左侧深色主导航（design-system §4）-->
      <aside class="mido-nav">
        <el-menu
          :default-active="activeMenu"
          class="mido-nav__menu"
          router
          background-color="transparent"
        >
          <el-menu-item
            v-for="item in navRoutes"
            :key="item.name"
            :index="'/' + item.path"
          >
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区（视图容器）-->
      <main class="mido-main">
        <router-view />
      </main>
    </div>

    <!-- 右侧详情抽屉容器（design-system §4：详情一律右抽屉，禁整页跳转）-->
    <el-drawer
      v-model="drawerVisible"
      :size="drawerSize"
      :with-header="true"
      title="详情"
      direction="rtl"
    >
      <el-empty description="右侧详情抽屉容器（占位）" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Bell, Grid } from '@element-plus/icons-vue'
import { navRoutes } from '@/router'

const route = useRoute()
const activeMenu = computed(() => route.path)

// 全局右抽屉容器，默认关闭；详情页后续注入内容。
const drawerVisible = ref(false)
const drawerSize = 'var(--mido-drawer-width)'
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
}

.mido-nav__menu {
  border-right: none;
}

/* 深色导航的 Element Plus 菜单换肤（仅用 tokens）*/
.mido-nav__menu :deep(.el-menu-item) {
  color: var(--mido-nav-text);
}

.mido-nav__menu :deep(.el-menu-item.is-active) {
  color: var(--mido-nav-text-active);
  background-color: var(--mido-nav-active-bg);
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
