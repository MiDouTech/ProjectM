<template>
  <!-- 选人占位组件：成员/部门/角色三态选人（design-system §5.2），实现待 Step 1。 -->
  <el-select
    :model-value="modelValue"
    placeholder="选择成员/部门/角色（占位）"
    disabled
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
defineProps({
  modelValue: { type: [String, Number, Array], default: null },
})
defineEmits(['update:modelValue'])
</script>
