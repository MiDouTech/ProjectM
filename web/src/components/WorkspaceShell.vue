<template>
  <!-- 一级模块统一顶部横向导航（可配置，来自 /workspace/nav）。L2：活动顶级项的子菜单渲染为第二行。 -->
  <div class="wsh-wrap">
    <nav class="wsh">
      <el-menu :default-active="activeTopCode" mode="horizontal" :ellipsis="true" class="wsh__menu" @select="onSelectTop">
        <el-menu-item v-for="n in nav" :key="n.code" :index="n.code">
          <el-icon v-if="n.icon"><component :is="n.icon" /></el-icon>
          <span>{{ n.name }}</span>
        </el-menu-item>
      </el-menu>
      <span v-if="$slots.actions" class="wsh__actions"><slot name="actions" /></span>
    </nav>
    <nav v-if="subNav.length" class="wsh-sub">
      <a v-for="c in subNav" :key="c.code" class="wsh-sub__tab"
        :class="{ 'is-active': matches(c) }" @click="go(c)">{{ c.name }}</a>
    </nav>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { workspaceNavApi } from '@/api/view'

const props = defineProps({ module: { type: String, required: true } })
const route = useRoute()
const router = useRouter()
const nav = ref([])

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
.wsh-wrap {
  margin-bottom: var(--mido-space-4);
}
.wsh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.wsh__menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}
.wsh__actions {
  flex: none;
  margin-left: var(--mido-space-3);
}
.wsh-sub {
  display: flex;
  gap: var(--mido-space-4);
  padding: var(--mido-space-2) 0;
}
.wsh-sub__tab {
  cursor: pointer;
  color: var(--el-text-color-regular);
  font-size: var(--mido-font-size-secondary);
}
.wsh-sub__tab.is-active {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
