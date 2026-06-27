<template>
  <!-- 统一错误态（design-system §8：空/加载/错误三态齐全）。
       接口失败时在内容区展示「加载失败 + 重试」，避免页面留白或停留旧数据。 -->
  <el-result icon="warning" :title="title" :sub-title="subTitle">
    <template #extra>
      <el-button type="primary" @click="$emit('retry')">重试</el-button>
    </template>
  </el-result>
</template>

<script setup>
defineProps({
  title: { type: String, default: '加载失败' },
  subTitle: { type: String, default: '请检查网络后重试' },
})
defineEmits(['retry'])
</script>
