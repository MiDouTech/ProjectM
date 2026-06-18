<template>
  <!-- 统一空状态（design-system §8：插画 + 引导 + 主操作）。
       业务列表/视图的空态优先用本组件，避免裸 el-empty 缺引导动作。 -->
  <el-empty :description="description" :image-size="imageSize">
    <el-button v-if="actionText" type="primary" :icon="actionIcon" @click="$emit('action')">
      {{ actionText }}
    </el-button>
    <slot />
  </el-empty>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  // 主操作按钮文案；不传则不渲染按钮（仅展示引导文案）
  actionText: { type: String, default: '' },
  actionIcon: { type: [Object, Function], default: null },
  imageSize: { type: Number, default: 80 },
})
defineEmits(['action'])
</script>
