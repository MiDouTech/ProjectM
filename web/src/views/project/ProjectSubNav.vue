<template>
  <!-- 项目域二级导航（全部项目 / 项目集）。项目集并入「项目」下，统一入口。 -->
  <nav class="psub">
    <router-link to="/project" class="psub__tab" :class="{ 'is-active': active === 'project' }">全部项目</router-link>
    <router-link to="/project/portfolios" class="psub__tab" :class="{ 'is-active': active === 'portfolios' }">项目集</router-link>
  </nav>
</template>

<script setup>
// active: 'project' | 'portfolios'
defineProps({ active: { type: String, default: 'project' } })
</script>

<style scoped>
.psub {
  display: flex;
  gap: var(--mido-space-5);
  margin-bottom: var(--mido-space-4);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.psub__tab {
  padding: var(--mido-space-2) 0;
  color: var(--el-text-color-regular);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  font-size: var(--mido-font-size-base);
}
.psub__tab.is-active {
  color: var(--el-color-primary);
  border-bottom-color: var(--el-color-primary);
  font-weight: 600;
}
</style>
