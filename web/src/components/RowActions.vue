<template>
  <!-- 行内操作列统一封装（design-system §8 危险操作隔离 / §9 热区）：
       统一操作间距；行内 link 文字按钮热区拔高到 ≥32px；危险操作(type=danger)自动加左分隔线 + 间距，
       与常规操作物理隔开、降低误点。用法：把操作列里的若干 <el-button> 包进本组件即可，danger 放最后。 -->
  <div class="mido-row-actions">
    <slot />
  </div>
</template>

<script setup></script>

<style scoped>
.mido-row-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-3);
}

/* 热区：行内 link 文字按钮拔高到 ≥32px 可点区（§9），不改字号/视觉 */
.mido-row-actions :deep(.el-button.is-link) {
  min-height: var(--mido-space-6);
}

/* 危险操作隔离（§8）：danger 与前序常规操作之间加左分隔线 + 间距；
   仅当其前面有同级操作时显示（:not(:first-child)），单独 danger 不出现孤立分隔。 */
.mido-row-actions :deep(.el-button--danger:not(:first-child)) {
  margin-left: var(--mido-space-2);
  padding-left: var(--mido-space-3);
  border-left: var(--mido-border-width) solid var(--el-border-color-light);
}
</style>
