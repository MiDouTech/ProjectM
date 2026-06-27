<template>
  <!-- 语义色色块（下拉选项预览用）：color 取 Element 语义色 token（primary/success/warning/danger/info） -->
  <span class="color-dot" :style="{ background: `var(--el-color-${color})` }" />
</template>

<script setup>
defineProps({ color: { type: String, default: 'info' } })
</script>

<style scoped>
.color-dot {
  display: inline-block;
  width: var(--mido-space-3);
  height: var(--mido-space-3);
  border-radius: 50%;
  vertical-align: middle;
  margin-right: var(--mido-space-2);
}
</style>
