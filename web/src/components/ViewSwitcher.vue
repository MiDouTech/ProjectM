<template>
  <!-- 视图切换占位组件（design-system §6：看板/列表/表格/甘特/日历/仪表盘）。 -->
  <el-radio-group
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-radio-button
      v-for="v in views"
      :key="v.value"
      :value="v.value"
    >{{ v.label }}</el-radio-button>
  </el-radio-group>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: 'kanban' },
})
defineEmits(['update:modelValue'])

// 占位：仅展示视图维度，渲染实现随各业务模块接入。
const views = [
  { value: 'kanban', label: '看板' },
  { value: 'list', label: '列表' },
  { value: 'table', label: '表格' },
  { value: 'gantt', label: '甘特' },
  { value: 'calendar', label: '日历' },
  { value: 'dashboard', label: '仪表盘' },
]
</script>
