<template>
  <!-- 一级模块统一顶部横向导航（可配置，来自 /workspace/nav）。右侧 actions 插槽放页面操作。 -->
  <nav class="wsh">
    <el-menu :default-active="activeCode" mode="horizontal" :ellipsis="true" class="wsh__menu" @select="onSelect">
      <el-menu-item v-for="n in nav" :key="n.code" :index="n.code">
        <el-icon v-if="n.icon"><component :is="n.icon" /></el-icon>
        <span>{{ n.name }}</span>
      </el-menu-item>
    </el-menu>
    <span v-if="$slots.actions" class="wsh__actions"><slot name="actions" /></span>
  </nav>
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
const activeCode = computed(() => (nav.value.find(matches) || {}).code || '')

function onSelect(code) {
  const n = nav.value.find((x) => x.code === code)
  if (n?.route && !matches(n)) router.push(n.route)
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
.wsh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  margin-bottom: var(--mido-space-4);
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
</style>
