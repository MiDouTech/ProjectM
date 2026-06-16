<template>
  <!-- 项目类型色标识 S/I/O（design-system §1.4 类型色 token）。
       类型色为品牌恒定语义，独立于状态着色（状态走 StatusTag）。 -->
  <span class="cat" :class="`cat--${cat.toLowerCase()}`">
    <span class="cat__code">{{ cat }}</span>
    <span v-if="showLabel" class="cat__label">{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  category: { type: String, default: '' },
  showLabel: { type: Boolean, default: true },
})

const LABELS = { S: '战略级', I: '创新级', O: '运营级' }
const cat = computed(() => (props.category || '').toUpperCase())
const label = computed(() => LABELS[cat.value] || cat.value)
</script>

<style scoped>
.cat {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
}
.cat__code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--mido-space-5);
  height: var(--mido-space-5);
  border-radius: var(--mido-radius-sm);
  color: var(--mido-nav-text-active);
  font-size: var(--mido-font-size-caption);
  font-weight: var(--mido-font-weight-bold);
}
.cat__label {
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-regular);
}
.cat--s .cat__code {
  background-color: var(--mido-cat-s);
}
.cat--i .cat__code {
  background-color: var(--mido-cat-i);
}
.cat--o .cat__code {
  background-color: var(--mido-cat-o);
}
</style>
