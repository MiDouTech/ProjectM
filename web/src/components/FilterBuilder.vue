<template>
  <!-- 多条件筛选器（design-system §6 表格视图「当…且…或」）。
       纯前端条件构造：输出 { match, rules[] }，由调用方对当前结果集求值。 -->
  <div class="fb">
    <div class="fb__head">
      <span class="mido-text-secondary">当满足以下</span>
      <el-select v-model="match" class="fb__match">
        <el-option label="全部（且）" value="and" />
        <el-option label="任一（或）" value="or" />
      </el-select>
      <span class="mido-text-secondary">条件：</span>
    </div>

    <div v-for="(rule, idx) in rules" :key="idx" class="fb__row">
      <el-select v-model="rule.field" placeholder="字段" class="fb__field" @change="onFieldChange(rule)">
        <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" />
      </el-select>
      <el-select v-model="rule.op" placeholder="条件" class="fb__op">
        <el-option v-for="o in opsOf(rule.field)" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-if="optionsOf(rule.field)" v-model="rule.value" placeholder="值" class="fb__value">
        <el-option v-for="opt in optionsOf(rule.field)" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input v-else v-model="rule.value" placeholder="值" class="fb__value" />
      <el-button link type="danger" @click="rules.splice(idx, 1)">删除</el-button>
    </div>

    <div class="fb__foot">
      <el-button link type="primary" :icon="Plus" @click="addRule">添加条件</el-button>
      <div class="fb__actions">
        <el-button v-if="rules.length" link @click="clear">清空</el-button>
        <el-button type="primary" @click="apply">应用筛选</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  // [{ value, label, type: 'text'|'number'|'enum', options?: [{value,label}] }]
  fields: { type: Array, required: true },
})
const emit = defineEmits(['apply'])

const match = ref('and')
const rules = reactive([])

const TEXT_OPS = [
  { value: 'contains', label: '包含' },
  { value: 'eq', label: '等于' },
  { value: 'neq', label: '不等于' },
]
const NUM_OPS = [
  { value: 'eq', label: '=' },
  { value: 'neq', label: '≠' },
  { value: 'gt', label: '>' },
  { value: 'gte', label: '≥' },
  { value: 'lt', label: '<' },
  { value: 'lte', label: '≤' },
]
const ENUM_OPS = [
  { value: 'eq', label: '等于' },
  { value: 'neq', label: '不等于' },
]

const fieldOf = (name) => props.fields.find((f) => f.value === name)
const opsOf = (name) => {
  const t = fieldOf(name)?.type
  if (t === 'number') return NUM_OPS
  if (t === 'enum') return ENUM_OPS
  return TEXT_OPS
}
const optionsOf = (name) => fieldOf(name)?.options || null

function addRule() {
  const f = props.fields[0]
  rules.push({ field: f.value, op: opsOf(f.value)[0].value, value: '' })
}
function onFieldChange(rule) {
  rule.op = opsOf(rule.field)[0].value
  rule.value = ''
}
function clear() {
  rules.splice(0, rules.length)
  emit('apply', { match: match.value, rules: [] })
}
function apply() {
  emit('apply', { match: match.value, rules: rules.filter((r) => r.value !== '' && r.value != null) })
}

defineExpose({ count: () => rules.length })
</script>

<style scoped>
.fb {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
  min-width: var(--mido-drawer-width);
  padding: var(--mido-space-1);
}
.fb__head {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.fb__match {
  width: var(--mido-admin-nav-width);
}
.fb__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.fb__field {
  width: var(--mido-admin-nav-width);
}
.fb__op {
  width: var(--mido-nav-width-collapsed);
  flex: none;
}
.fb__value {
  flex: 1;
  min-width: 0;
}
.fb__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fb__actions {
  display: flex;
  gap: var(--mido-space-2);
}
</style>
