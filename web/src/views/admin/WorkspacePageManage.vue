<template>
  <div class="mido-page wp">
    <div class="wp__bar">
      <h1 class="mido-h1">页面配置</h1>
      <div class="wp__bar-actions">
        <el-select v-model="target" class="wp__sel" @change="load">
          <el-option v-for="t in TARGETS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="templateType" class="wp__sel" @change="load">
          <el-option v-for="t in TEMPLATES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="columns" class="wp__sel">
          <el-option :value="1" label="单列" />
          <el-option :value="2" label="双列" />
        </el-select>
        <el-button @click="restoreDefault">恢复默认</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>
    <p class="mido-text-secondary">配置「{{ targetLabel }}」建/编辑表单的字段：勾选纳入、拖拽排序、设分组/必填/只读/宽度。空配置走默认（内置字段全显示）。</p>

    <div class="wp__body" v-loading="loading">
      <!-- 可选字段 -->
      <el-card shadow="never" class="wp__avail">
        <div class="wp__h">可选字段</div>
        <div v-for="f in available" :key="f.source + ':' + f.fieldKey" class="wp__opt">
          <el-checkbox :model-value="isSelected(f)" @change="(v) => toggle(f, v)">
            {{ f.label }}<span class="mido-text-secondary"> · {{ f.source === 'builtin' ? '内置' : '自定义' }} · {{ f.type }}</span>
          </el-checkbox>
        </div>
      </el-card>

      <!-- 已选编排 -->
      <el-card shadow="never" class="wp__sel-card">
        <div class="wp__h">已选字段 · {{ selected.length }}</div>
        <draggable v-model="selected" item-key="fieldKey" handle=".wp__drag" class="wp__list">
          <template #item="{ element }">
            <div class="wp__row">
              <el-icon class="wp__drag"><Rank /></el-icon>
              <span class="wp__label">{{ element.label }}</span>
              <el-input v-model="element.group" placeholder="分组" class="wp__group" />
              <el-checkbox v-model="element.required">必填</el-checkbox>
              <el-checkbox v-model="element.readonly">只读</el-checkbox>
              <el-select v-model="element.width" class="wp__width">
                <el-option :value="24" label="整行" />
                <el-option :value="12" label="半行" />
                <el-option :value="8" label="1/3" />
              </el-select>
              <el-button link type="danger" @click="toggle(element, false)">移除</el-button>
            </div>
          </template>
        </draggable>
        <el-empty v-if="!selected.length" description="未选择字段" :image-size="60" />
      </el-card>

      <!-- 预览 -->
      <el-card shadow="never" class="wp__preview">
        <div class="wp__h">预览</div>
        <DynamicForm v-if="templateType === 'form'" :fields="selected" :model-value="previewModel" :layout="{ columns }" />
        <DynamicDetail v-else :fields="selected" :model-value="previewModel" :layout="{ columns }" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import DynamicForm from '@/components/DynamicForm.vue'
import DynamicDetail from '@/components/DynamicDetail.vue'
import { pageConfigApi } from '@/api/view'
import { fieldDefApi } from '@/api/field'
import { parseFieldOptions } from '@/utils/pageConfig'

const TARGETS = [
  { value: 'task', label: '任务' },
  { value: 'project', label: '项目' },
]
const TEMPLATES = [
  { value: 'form', label: '表单' },
  { value: 'detail', label: '详情' },
]

const target = ref('task')
const templateType = ref('form')
const columns = ref(1)
const loading = ref(false)
const saving = ref(false)
const available = ref([])
const selected = ref([])
const previewModel = reactive({})
const targetLabel = computed(() => TARGETS.find((t) => t.value === target.value)?.label || target.value)

const isSelected = (f) => selected.value.some((s) => s.fieldKey === f.fieldKey && s.source === f.source)
const findAvail = (fieldKey, source) =>
  available.value.find((f) => f.fieldKey === fieldKey && f.source === source)

function toRow(f, cfg = {}) {
  return {
    fieldKey: f.fieldKey, source: f.source, label: f.label, type: f.type, options: f.options || [],
    group: cfg.group || '', required: cfg.required ?? f.requiredDefault ?? false,
    readonly: cfg.readonly ?? false, width: cfg.width || 24,
  }
}
function toggle(f, v) {
  if (v) {
    if (!isSelected(f)) selected.value.push(toRow(findAvail(f.fieldKey, f.source) || f))
  } else {
    selected.value = selected.value.filter((s) => !(s.fieldKey === f.fieldKey && s.source === f.source))
  }
}

async function loadAvailable() {
  const [builtin, custom] = await Promise.all([
    pageConfigApi.fields(target.value),
    fieldDefApi.list(target.value, true).catch(() => []),
  ])
  const a = (builtin || []).map((f) => ({
    fieldKey: f.key, source: 'builtin', label: f.label, type: f.type, requiredDefault: f.required,
  }))
  ;(custom || []).forEach((c) => a.push({
    fieldKey: c.fieldKey, source: 'custom', label: c.name, type: c.type,
    options: parseFieldOptions(c.options), requiredDefault: c.required === 1,
  }))
  available.value = a
}

async function load() {
  loading.value = true
  try {
    await loadAvailable()
    const cfg = await pageConfigApi.get(target.value, templateType.value)
    if (cfg && Array.isArray(cfg.fields) && cfg.fields.length) {
      columns.value = cfg.layout?.columns || 1
      selected.value = cfg.fields
        .map((c) => {
          const f = findAvail(c.fieldKey, c.source)
          return f ? toRow(f, c) : null
        })
        .filter(Boolean)
    } else {
      restoreDefault()
    }
  } finally {
    loading.value = false
  }
}

function restoreDefault() {
  // 默认：全部内置字段，按目录顺序
  selected.value = available.value.filter((f) => f.source === 'builtin').map((f) => toRow(f))
}

async function save() {
  saving.value = true
  try {
    const config = {
      layout: { columns: columns.value },
      fields: selected.value.map((s) => ({
        fieldKey: s.fieldKey, source: s.source, group: s.group || null,
        required: s.required, readonly: s.readonly, width: s.width,
      })),
    }
    await pageConfigApi.save(target.value, templateType.value, config)
    ElMessage.success('页面配置已保存')
  } finally {
    saving.value = false
  }
}

load()
</script>

<style scoped>
.wp__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wp__bar-actions {
  display: flex;
  gap: var(--mido-space-2);
}
.wp__sel {
  width: 120px;
}
.wp__body {
  display: flex;
  gap: var(--mido-space-3);
  margin-top: var(--mido-space-3);
  align-items: flex-start;
}
.wp__avail {
  width: 240px;
  flex-shrink: 0;
}
.wp__sel-card {
  flex: 1;
  min-width: 0;
}
.wp__preview {
  width: 360px;
  flex-shrink: 0;
}
.wp__h {
  font-weight: 600;
  margin-bottom: var(--mido-space-2);
}
.wp__opt {
  padding: var(--mido-space-1) 0;
}
.wp__list {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.wp__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-1) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-lighter);
}
.wp__drag {
  cursor: move;
  color: var(--el-text-color-secondary);
}
.wp__label {
  width: 96px;
}
.wp__group {
  width: 90px;
}
.wp__width {
  width: 90px;
}
</style>
