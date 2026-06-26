<template>
  <div class="cost" v-loading="loading">
    <div class="cost__bar">
      <span class="mido-text-secondary">共 {{ rows.length }} 条 · 预算合计
        <b class="mido-mono">{{ money(sumBudget) }}</b> · 执行合计
        <b class="mido-mono">{{ money(sumActual) }}</b></span>
      <div class="cost__bar-actions">
        <TableColumnSetting list-key="costs" :all-columns="COST_COLUMNS"
          :default-columns="COST_DEFAULT_COLS" @change="onCostColsChange" />
        <el-button link type="primary" :icon="Download" @click="exportCsv">导出</el-button>
        <el-button link type="primary" :icon="Plus" @click="openCreate">新增费用</el-button>
      </div>
    </div>

    <el-table :data="rows" size="small">
      <el-table-column v-for="key in costCols" :key="key" :label="costColLabel(key)"
        :prop="costColProp(key)" :width="costColWidth(key)" :min-width="costColMinWidth(key)"
        :align="key === 'budgetAmount' || key === 'actualAmount' ? 'right' : undefined"
        :fixed="costFrozen.includes(key) ? 'left' : false" sortable>
        <template #default="{ row }">
          <span v-if="key === 'budgetAmount' || key === 'actualAmount'" class="mido-mono">{{ money(row[key]) }}</span>
          <StatusTag v-else-if="key === 'status'" :status="row.status" />
          <span v-else>{{ row[key] ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== '已发生'" link type="primary" @click="submit(row)">提报</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无费用" :image-size="60" /></template>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑费用' : '新增费用'" width="480">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="科目" prop="account">
          <el-select v-model="form.account" filterable allow-create placeholder="选择或输入科目">
            <el-option v-for="a in COST_ACCOUNTS" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算" prop="budgetAmount">
          <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="执行">
          <el-input-number v-model="form.actualAmount" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="发生日"><el-date-picker v-model="form.occurDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="付款日"><el-date-picker v-model="form.payDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import StatusTag from './StatusTag.vue'
import TableColumnSetting from './TableColumnSetting.vue'
import { costApi, COST_ACCOUNTS } from '@/api/cost'

const props = defineProps({
  projectId: { type: [Number, String], default: null },
})

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const dialog = ref(false)
const formRef = ref()
const form = reactive({ id: null, title: '', account: '', budgetAmount: 0, actualAmount: null, occurDate: null, payDate: null })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  account: [{ required: true, message: '请选择科目', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算', trigger: 'blur' }],
}

// 列头可配置（对齐 Worktile：可勾选显隐）
const COST_COLUMNS = [
  { key: 'title', label: '标题', required: true },
  { key: 'account', label: '科目' },
  { key: 'budgetAmount', label: '预算' },
  { key: 'actualAmount', label: '执行' },
  { key: 'occurDate', label: '发生日' },
  { key: 'payDate', label: '付款日' },
  { key: 'status', label: '状态' },
]
const COST_DEFAULT_COLS = ['title', 'account', 'budgetAmount', 'actualAmount', 'status']
const COST_COL_META = {
  title: { minWidth: 140 },
  account: { width: 90 },
  budgetAmount: { width: 110 },
  actualAmount: { width: 110 },
  occurDate: { width: 120 },
  payDate: { width: 120 },
  status: { width: 90 },
}
const costCols = ref([...COST_DEFAULT_COLS])
const costFrozen = ref([])
const costColLabel = (key) => COST_COLUMNS.find((c) => c.key === key)?.label || key
const costColWidth = (key) => COST_COL_META[key]?.width
const costColMinWidth = (key) => COST_COL_META[key]?.minWidth
// 自定义渲染列（金额/状态）不设 prop，避免 el-table 默认按对象渲染
const costColProp = (key) => (['budgetAmount', 'actualAmount', 'status'].includes(key) ? undefined : key)
function onCostColsChange({ columns: cols, frozen }) {
  costCols.value = cols
  costFrozen.value = frozen
}

const money = (v) => Number(v || 0).toFixed(2)
const sumBudget = computed(() => rows.value.reduce((s, r) => s + Number(r.budgetAmount || 0), 0))
const sumActual = computed(() => rows.value.reduce((s, r) => s + Number(r.actualAmount || 0), 0))

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    rows.value = await costApi.listByProject(props.projectId)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', account: '', budgetAmount: 0, actualAmount: null, occurDate: null, payDate: null })
  formRef.value?.clearValidate()
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, {
    id: row.id, title: row.title, account: row.account,
    budgetAmount: Number(row.budgetAmount || 0),
    actualAmount: row.actualAmount == null ? null : Number(row.actualAmount),
    occurDate: row.occurDate, payDate: row.payDate,
  })
  dialog.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const body = {
      title: form.title, account: form.account, budgetAmount: form.budgetAmount,
      actualAmount: form.actualAmount, occurDate: form.occurDate, payDate: form.payDate,
    }
    if (form.id) {
      await costApi.update(form.id, body)
    } else {
      await costApi.create({ projectId: props.projectId, ...body })
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function submit(row) {
  await costApi.submit(row.id)
  ElMessage.success('已提报审批')
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除费用「${row.title}」?`, '提示', { type: 'warning' })
  await costApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

function exportCsv() {
  const header = costCols.value.map((k) => costColLabel(k))
  const lines = rows.value.map((r) => costCols.value.map((k) => csvCell(r[k])).join(','))
  const csv = [header.join(','), ...lines].join('\n')
  // 加 BOM 保证 Excel 正确识别 UTF-8 中文
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `费用_项目${props.projectId}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}
function csvCell(v) {
  const s = v == null ? '' : String(v)
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

watch(() => props.projectId, load, { immediate: true })
</script>

<style scoped>
.cost__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.cost__bar-actions {
  display: flex;
  gap: var(--mido-space-3);
}
</style>
