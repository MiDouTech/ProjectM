<template>
  <!-- 自定义字段：按实体(entityType/entityId)渲染租户自配字段，支持按类型编辑后批量保存。
       字段定义由租户在配置页维护（P2-4b）；无启用字段时整块隐藏。 -->
  <div v-if="fields.length" v-loading="loading" class="cf">
    <el-form :label-width="100" class="cf__form">
      <el-form-item
        v-for="f in fields"
        :key="f.fieldId"
        :label="f.name"
        :required="f.required"
      >
        <el-input v-if="f.type === 'text'" v-model="model[f.fieldId]" placeholder="请输入" />
        <el-input v-else-if="f.type === 'number'" v-model="model[f.fieldId]" placeholder="请输入数字" />
        <el-date-picker
          v-else-if="f.type === 'date'"
          v-model="model[f.fieldId]"
          type="date"
          value-format="YYYY-MM-DD"
          class="cf__full"
        />
        <el-switch v-else-if="f.type === 'checkbox'" v-model="model[f.fieldId]" />
        <el-select
          v-else-if="f.type === 'select'"
          v-model="model[f.fieldId]"
          clearable
          placeholder="请选择"
          class="cf__full"
        >
          <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select
          v-else-if="f.type === 'multi_select'"
          v-model="model[f.fieldId]"
          multiple
          clearable
          placeholder="请选择"
          class="cf__full"
        >
          <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <UserSelect v-else-if="f.type === 'user'" v-model="model[f.fieldId]" placeholder="选择用户" />
        <el-input v-else v-model="model[f.fieldId]" placeholder="请输入" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存自定义字段</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import UserSelect from '@/components/UserSelect.vue'
import { fieldValueApi } from '@/api/field'

const props = defineProps({
  entityType: { type: String, required: true },
  entityId: { type: [Number, String], default: null },
})
const emit = defineEmits(['changed'])

const loading = ref(false)
const saving = ref(false)
const fields = ref([])
const model = reactive({})

watch(() => [props.entityType, props.entityId], () => reload(), { immediate: true })

async function reload() {
  if (!props.entityId) return
  loading.value = true
  try {
    const rows = await fieldValueApi.list(props.entityType, props.entityId)
    fields.value = rows
    Object.keys(model).forEach((k) => delete model[k])
    rows.forEach((f) => { model[f.fieldId] = parseValue(f) })
  } finally {
    loading.value = false
  }
}

/** 入库字符串 → 编辑态 */
function parseValue(f) {
  const v = f.value
  if (f.type === 'checkbox') return v === 'true'
  if (f.type === 'multi_select') {
    if (!v) return []
    try { return JSON.parse(v) } catch { return [] }
  }
  return v ?? ''
}

/** 编辑态 → 入库字符串契约 */
function toStored(f, val) {
  if (f.type === 'checkbox') return val ? 'true' : 'false'
  if (f.type === 'multi_select') return Array.isArray(val) && val.length ? JSON.stringify(val) : ''
  if (val === null || val === undefined) return ''
  return String(val)
}

async function save() {
  const values = fields.value.map((f) => ({ fieldId: f.fieldId, value: toStored(f, model[f.fieldId]) }))
  saving.value = true
  try {
    await fieldValueApi.save({ entityType: props.entityType, entityId: props.entityId, values })
    ElMessage.success('已保存')
    emit('changed')
    reload()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cf__full { width: 100%; }
</style>
