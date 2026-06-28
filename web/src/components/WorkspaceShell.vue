<template>
  <!-- 一级模块统一二级导航（可配置，来自 /workspace/nav）。
       L2 横向菜单经 Teleport 并入顶栏中段（design-system §4）；actions/L3 子导航留内容区顶部。 -->
  <Teleport to="#mido-topbar-nav" :disabled="!teleportReady">
    <el-menu :default-active="activeTopCode" mode="horizontal" :ellipsis="true"
      class="wsh__menu" background-color="transparent" @select="onSelectTop">
      <el-menu-item v-for="n in nav" :key="n.code" :index="n.code">
        <el-icon v-if="n.icon"><component :is="n.icon" /></el-icon>
        <span>{{ n.name }}</span>
      </el-menu-item>
    </el-menu>
  </Teleport>
  <div v-if="$slots.actions || subNav.length" class="wsh-sub-wrap">
    <nav v-if="subNav.length" class="wsh-sub">
      <a v-for="c in subNav" :key="c.code" class="mido-l2-tab"
        :class="{ 'is-active': matches(c) }" @click="go(c)">{{ c.name }}</a>
    </nav>
    <span v-if="$slots.actions" class="wsh__actions"><slot name="actions" /></span>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { workspaceNavApi } from '@/api/view'

const props = defineProps({ module: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const nav = ref([])
// Teleport 目标(#mido-topbar-nav)在 MainLayout，父级先挂载即存在；挂载后再启用，避免首帧目标未就绪
const teleportReady = ref(false)
onMounted(() => { teleportReady.value = true })

// 命中判定：路径相等；组件带 query(如 ?tab=change) 则要求 query 命中，否则要求当前无 tab query
function matches(n) {
  const [path, query] = (n.route || '').split('?')
  if (route.path !== path) return false
  if (query) return route.fullPath.includes(query)
  return !route.query.tab
}
// 活动顶级项：自身命中，或其任一子项命中
const activeTop = computed(() =>
  nav.value.find((n) => matches(n) || (n.children || []).some(matches)) || null)
const activeTopCode = computed(() => activeTop.value?.code || '')
const subNav = computed(() => activeTop.value?.children || [])

function go(n) {
  if (n?.route && !matches(n)) router.push(n.route)
}
function onSelectTop(code) {
  const n = nav.value.find((x) => x.code === code)
  // 有子菜单的顶级项：进入其首个子项；否则进入自身路由
  const target = (n?.children && n.children[0]) || n
  go(target)
}

async function load() {
  try {
    nav.value = (await workspaceNavApi.nav(props.module)) || []
  } catch {
    nav.value = []
  }
}
load()
</script>

<style scoped>
/* L2 菜单内嵌顶栏：撑满中段、贴合顶栏高度、去底边与背景 */
.wsh__menu {
  flex: 1;
  min-width: 0;
  height: var(--mido-topbar-height);
  border-bottom: none;
  background: transparent;
}
.wsh__menu :deep(.el-menu-item) {
  height: var(--mido-topbar-height);
  line-height: var(--mido-topbar-height);
}
/* actions/L3 子导航留内容区顶部，整体在与正文之间留一档间距 */
.wsh-sub-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
}
.wsh__actions {
  flex: none;
}
.wsh-sub {
  display: flex;
  gap: var(--mido-space-4);
  padding: var(--mido-space-2) 0;
}
/* 二级横向文字 Tab 当前态统一走全局 .mido-l2-tab（见 global.css），此处不再重复样式 */
</style>
