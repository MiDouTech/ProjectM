<template>
  <!-- 视图切换器（design-system §5.2/§6：看板/列表/表格/甘特/日历/仪表盘）。
       默认六视图；调用方可传 views 收敛为子集（如项目列表用 列表/表格/卡片）。 -->
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
  modelValue: { type: String, default: 'list' },
  // 可选：收敛视图集合；不传则用默认六视图（看板/列表/表格/甘特/日历/仪表盘）
  views: {
    type: Array,
    default: () => [
      { value: 'kanban', label: '看板' },
      { value: 'list', label: '列表' },
      { value: 'table', label: '表格' },
      { value: 'gantt', label: '甘特' },
      { value: 'calendar', label: '日历' },
      { value: 'dashboard', label: '仪表盘' },
    ],
  },
})
defineEmits(['update:modelValue'])
</script>
