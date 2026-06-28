<template>
  <!-- 批量工具条（design-system §5.2 / review §4）：列表多选后浮于表上方，统一「已选 N + 批量动作」。
       两端共享：租户列表与运营后台 ops 均复用，收敛原先发散实现。动作经默认插槽传入，按权限置灰由调用方控制。 -->
  <div v-if="count > 0" class="mido-batchbar" role="region" aria-label="批量操作">
    <span class="mido-batchbar__info">已选 {{ count }} {{ unit }}</span>
    <slot />
    <el-button v-if="clearable" link class="mido-batchbar__clear" @click="$emit('clear')">取消选择</el-button>
  </div>
</template>

<script setup>
defineProps({
  // 选中条数；为 0 时整条不渲染
  count: { type: Number, default: 0 },
  // 计量单位文案（如「项」「个租户」）
  unit: { type: String, default: '项' },
  // 是否显示「取消选择」链接（需调用方监听 @clear 清空选择）
  clearable: { type: Boolean, default: false },
})
defineEmits(['clear'])
</script>

<style scoped>
.mido-batchbar {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2) var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
  background-color: var(--el-fill-color-light);
  border-radius: var(--mido-radius-md);
}
.mido-batchbar__info {
  margin-right: auto;
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-regular);
}
</style>
