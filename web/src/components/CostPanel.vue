<template>
  <div class="cost" v-loading="loading">
    <div class="cost__bar">
      <span class="mido-text-secondary">共 {{ rows.length }} 条 · 预算合计
        <b class="mido-mono">{{ money(sumBudget) }}</b> · 执行合计
        <b class="mido-mono">{{ money(sumActual) }}</b></span>
      <div class="cost__bar-actions">
        <!-- 列头可配置 -->
        <el-dropdown trigger="click" :hide-on-click="false">
          <el-button link type="primary" :icon="Setting">列设置</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="col in columns" :key="col.key">
                <el-checkbox v-model="col.visible">{{ col.label }}</el-checkbox>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button link type="primary" :icon="Download" @click="exportCsv">导出</el-button>
        <el-button link type="primary" :icon="Plus" @click="openCreate">新增费用</el-button>
      </div>
    </div>

    <el-table :data="rows" size="small">
      <el-table-column v-if="col('title').visible" prop="title" label="标题" min-width="140" show-overflow-tooltip sortable />
      <el-table-column v-if="col('account').visible" prop="account" label="科目" width="90" sortable />
      <el-table-column v-if="col('budgetAmount').visible" label="预算" width="110" align="right" sortable :sort-by="(row) => row.budgetAmount">
        <template #default="{ row }"><span class="mido-mono">{{ money(row.budgetAmount) }}</span></template>
      </el-table-column>
      <el-table-column v-if="col('actualAmount').visible" label="执行" width="110" align="right" sortable :sort-by="(row) => row.actualAmount">
        <template #default="{ row }"><span class="mido-mono">{{ money(row.actualAmount) }}</span></template>
      </el-table-column>
      <el-table-column v-if="col('occurDate').visible" prop="occurDate" label="发生日" width="120" sortable />
      <el-table-column v-if="col('payDate').visible" prop="payDate" label="付款日" width="120" sortable />
      <el-table-column v-if="col('status').visible" label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
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
import { Download, Plus, Setting } from '@element-plus/icons-vue'
import StatusTag from './StatusTag.vue'
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
const columns = ref([
  { key: 'title', label: '标题', visible: true },
  { key: 'account', label: '科目', visible: true },
  { key: 'budgetAmount', label: '预算', visible: true },
  { key: 'actualAmount', label: '执行', visible: true },
  { key: 'occurDate', label: '发生日', visible: false },
  { key: 'payDate', label: '付款日', visible: false },
  { key: 'status', label: '状态', visible: true },
])
const col = (key) => columns.value.find((c) => c.key === key)

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
  const visible = columns.value.filter((c) => c.visible)
  const header = visible.map((c) => c.label)
  const lines = rows.value.map((r) => visible.map((c) => csvCell(r[c.key])).join(','))
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
