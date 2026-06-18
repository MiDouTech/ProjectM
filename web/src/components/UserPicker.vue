<template>
  <!-- 选人组件（design-system §5.2）：成员可搜索单/多选。
       数据走统一入口 fetchMembers；雪花 ID 以字符串透传，避免前端精度丢失。 -->
  <el-select
    :model-value="modelValue"
    :multiple="multiple"
    filterable
    clearable
    :placeholder="placeholder"
    :loading="loading"
    class="up"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option v-for="u in members" :key="u.id" :label="label(u)" :value="String(u.id)" />
  </el-select>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchMembers } from '@/api/org'

defineProps({
  // 单选：String/Number；多选：Array
  modelValue: { type: [String, Number, Array], default: null },
  multiple: { type: Boolean, default: false },
  placeholder: { type: String, default: '选择成员' },
})
defineEmits(['update:modelValue'])

const members = ref([])
const loading = ref(false)
const label = (u) => u.name || u.username || `用户#${u.id}`

onMounted(async () => {
  loading.value = true
  try {
    members.value = await fetchMembers()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.up {
  width: 100%;
}
</style>
