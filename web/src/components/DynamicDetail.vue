<template>
  <!-- 配置驱动的只读详情（ADR-0004 · L3.2）。按 group 归组、按字段展示值。 -->
  <div class="dd">
    <template v-for="g in groups" :key="g.name">
      <div v-if="g.name" class="dd__group">{{ g.name }}</div>
      <el-descriptions :column="layout?.columns || 1" border size="small" class="dd__desc">
        <el-descriptions-item v-for="f in g.fields" :key="f.fieldKey" :label="f.label">
          {{ display(f) }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-if="!fields.length" description="未配置字段" :image-size="60" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 解析后的字段：[{ fieldKey, label, type, group, options? }]
  fields: { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
  layout: { type: Object, default: () => ({ columns: 1 }) },
  // 可选：用户 id→名称解析（user 类型字段展示用）
  userName: { type: Function, default: (v) => (v == null ? '—' : `用户#${v}`) },
})

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

function display(f) {
  const v = props.modelValue?.[f.fieldKey]
  if (v == null || v === '') return '—'
  if (f.type === 'user') return props.userName(v)
  if (f.type === 'checkbox') return v ? '是' : '否'
  if ((f.type === 'select' || f.type === 'multi_select') && f.options) {
    const arr = Array.isArray(v) ? v : [v]
    return arr.map((x) => f.options.find((o) => o.value === x)?.label ?? x).join('、')
  }
  return String(v)
}
</script>

<style scoped>
.dd__group {
  margin: var(--mido-space-3) 0 var(--mido-space-2);
  font-weight: 600;
}
.dd__desc {
  margin-bottom: var(--mido-space-2);
}
</style>
