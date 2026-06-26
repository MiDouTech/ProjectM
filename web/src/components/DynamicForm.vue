<template>
  <!-- 配置驱动的动态表单（ADR-0004 · L3）。fields 已是解析后的字段（含 type/required/readonly/width/group）。 -->
  <el-form ref="formRef" :model="modelValue" label-width="92px">
    <template v-for="g in groups" :key="g.name">
      <div v-if="g.name" class="df__group">{{ g.name }}</div>
      <el-row :gutter="16">
        <el-col v-for="f in g.fields" :key="f.fieldKey" :span="colSpan(f)">
          <el-form-item :label="f.label" :prop="f.fieldKey" :rules="ruleOf(f)">
            <el-input v-if="f.type === 'text'" v-model="modelValue[f.fieldKey]" :disabled="f.readonly"
              :type="f.fieldKey === 'description' ? 'textarea' : 'text'" />
            <el-input-number v-else-if="f.type === 'number'" v-model="modelValue[f.fieldKey]"
              :disabled="f.readonly" :controls="false" class="df__full" />
            <el-date-picker v-else-if="f.type === 'date'" v-model="modelValue[f.fieldKey]" type="date"
              value-format="YYYY-MM-DD" :disabled="f.readonly" class="df__full" />
            <el-select v-else-if="f.type === 'select'" v-model="modelValue[f.fieldKey]" clearable
              :disabled="f.readonly" class="df__full">
              <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-select v-else-if="f.type === 'multi_select'" v-model="modelValue[f.fieldKey]" multiple clearable
              :disabled="f.readonly" class="df__full">
              <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-switch v-else-if="f.type === 'checkbox'" v-model="modelValue[f.fieldKey]" :disabled="f.readonly" />
            <UserSelect v-else-if="f.type === 'user'" v-model="modelValue[f.fieldKey]" :disabled="f.readonly" />
            <el-input v-else v-model="modelValue[f.fieldKey]" :disabled="f.readonly" />
          </el-form-item>
        </el-col>
      </el-row>
    </template>
  </el-form>
</template>

<script setup>
import { computed, ref } from 'vue'
import UserSelect from '@/components/UserSelect.vue'

const props = defineProps({
  // 解析后的字段：[{ fieldKey, label, type, required, readonly, width, group, options? }]
  fields: { type: Array, default: () => [] },
  // 表单数据（须为 reactive 对象，组件直接双绑其属性）
  modelValue: { type: Object, default: () => ({}) },
  layout: { type: Object, default: () => ({ columns: 1 }) },
})

const formRef = ref()

// 按 group 归组（保序）；无 group 归入空组
const groups = computed(() => {
  const order = []
  const map = new Map()
  for (const f of props.fields) {
    const key = f.group || ''
    if (!map.has(key)) {
      map.set(key, { name: key, fields: [] })
      order.push(key)
    }
    map.get(key).fields.push(f)
  }
  return order.map((k) => map.get(k))
})

const colSpan = (f) => {
  if (f.width) return Number(f.width)
  return (props.layout?.columns || 1) >= 2 ? 12 : 24
}
const ruleOf = (f) =>
  f.required ? [{ required: true, message: `请填写${f.label}`, trigger: 'blur' }] : []

defineExpose({ validate: () => formRef.value.validate() })
</script>

<style scoped>
.df__group {
  margin: var(--mido-space-3) 0 var(--mido-space-2);
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.df__full {
  width: 100%;
}
</style>
