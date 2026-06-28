<template>
  <!-- 管理后台独立全屏布局：脱离主应用深色左导航，顶栏承载两级横向导航，正文全宽。
       L1=功能分组（组织与权限/项目配置/…），L2=当前分组的子项。 -->
  <div class="mido-layout">
    <header class="mido-topbar">
      <div class="mido-topbar__brand" role="button" tabindex="0" @click="goApp" @keydown.enter="goApp">
        <el-icon class="mido-topbar__logo-fallback"><Setting /></el-icon>
        <span class="mido-h2">管理后台</span>
      </div>
      <!-- L1 分组横向导航 -->
      <nav class="ash__groups">
        <el-menu :default-active="activeGroup" mode="horizontal" :ellipsis="true"
          background-color="transparent" class="ash__menu" @select="onSelectGroup">
          <el-menu-item v-for="g in groups" :key="g.code" :index="g.code">{{ g.name }}</el-menu-item>
        </el-menu>
      </nav>
      <div class="mido-topbar__actions">
        <el-button link class="ash__back" @click="goApp">
          <el-icon><Back /></el-icon><span>返回应用</span>
        </el-button>
        <el-dropdown @command="onUserCommand">
          <el-avatar class="mido-topbar__avatar">{{ myInitial }}</el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="app">返回应用</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- L2 当前分组子项横向菜单 -->
    <nav class="ash__sub">
      <a v-for="it in subItems" :key="it.path" class="ash__tab"
        :class="{ 'is-active': route.path === it.path }" @click="go(it.path)">{{ it.name }}</a>
    </nav>

    <main class="ash__content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting, Back } from '@element-plus/icons-vue'
import { adminNavGroups } from '@/router'
import { useUserStore } from '@/store/user'
import { useMe } from '@/composables/useMe'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 按功能码过滤子项（如开放平台需 openapi）。features 未取到时 hasFeature 走 fail-open。
const groups = computed(() =>
  adminNavGroups
    .map((g) => ({ ...g, children: g.children.filter((c) => !c.feature || userStore.hasFeature(c.feature)) }))
    .filter((g) => g.children.length),
)
// 当前路由所属分组；找不到回落首组
const activeGroup = computed(() => {
  const g = groups.value.find((x) => x.children.some((c) => c.path === route.path))
  return (g || groups.value[0])?.code || ''
})
const subItems = computed(() => groups.value.find((g) => g.code === activeGroup.value)?.children || [])

function onSelectGroup(code) {
  const g = groups.value.find((x) => x.code === code)
  const first = g?.children[0]
  if (first && first.path !== route.path) router.push(first.path)
}
function go(path) {
  if (path !== route.path) router.push(path)
}
function goApp() {
  router.push('/')
}

// 顶栏头像首字：取当前用户名首字，失败回落「管」
const { initial: myInitial } = useMe('管')
function onUserCommand(command) {
  if (command === 'app') goApp()
  else if (command === 'logout') {
    userStore.clearToken()
    router.push('/login')
  }
}

onMounted(() => {
  userStore.fetchFeatures()
})
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
  cursor: pointer;
}
.mido-topbar__logo-fallback {
  font-size: var(--mido-font-size-h1);
}

/* L1 分组导航：撑满中段，贴合顶栏高度，去底边与背景 */
.ash__groups {
  flex: 1;
  min-width: 0;
  margin-left: var(--mido-space-4);
  padding-left: var(--mido-space-4);
  border-left: var(--mido-border-width) solid var(--el-border-color-lighter);
}
.ash__menu {
  height: var(--mido-topbar-height);
  border-bottom: none;
  background: transparent;
}
.ash__menu :deep(.el-menu-item) {
  height: var(--mido-topbar-height);
  line-height: var(--mido-topbar-height);
}
/* hover 底色对齐下方列表行 hover（浅灰），不再用默认深灰块 */
.ash__menu :deep(.el-menu-item):not(.is-disabled):hover,
.ash__menu :deep(.el-menu-item):not(.is-disabled):focus {
  background-color: var(--el-fill-color-light);
}
/* 选中态：深底改为品牌浅蓝点睛，配合主色文字+底部主色描边，柔和不抢眼 */
.ash__menu :deep(.el-menu-item.is-active),
.ash__menu :deep(.el-menu-item.is-active):hover,
.ash__menu :deep(.el-menu-item.is-active):focus {
  background-color: var(--el-color-primary-light-9);
}

.mido-topbar__actions {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
}
.ash__back {
  color: var(--el-text-color-regular);
}
.mido-topbar__avatar {
  width: var(--mido-space-6);
  height: var(--mido-space-6);
  background-color: var(--el-color-primary);
  cursor: pointer;
}

/* L2 子项导航条：浅色描边底，横向 Tab */
.ash__sub {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--mido-space-5);
  padding: var(--mido-space-3) var(--mido-space-5);
  background-color: var(--el-bg-color);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.ash__tab {
  cursor: pointer;
  color: var(--el-text-color-regular);
  font-size: var(--mido-font-size-secondary);
  padding-bottom: var(--mido-space-1);
  border-bottom: 2px solid transparent;
}
.ash__tab:hover {
  color: var(--el-color-primary);
}
.ash__tab.is-active {
  color: var(--el-color-primary);
  font-weight: 600;
  border-bottom-color: var(--el-color-primary);
}

.ash__content {
  flex: 1;
  min-width: 0;
  padding: var(--mido-space-5);
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}
</style>
