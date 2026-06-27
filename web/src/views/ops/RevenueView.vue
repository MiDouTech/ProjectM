<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">收入台账</h2>
      <div class="bar__right">
        <el-button :icon="Download" :disabled="!rows.length" @click="doExport">导出本页</el-button>
        <el-button type="primary" :icon="Plus" :disabled="!ops.hasPerm('platform:revenue:manage')" @click="openCreate">新增记录</el-button>
      </div>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-skeleton v-else-if="loading && !rows.length" :rows="6" animated :throttle="300" />
    <template v-else>
    <!-- 汇总卡片 -->
    <div class="summary">
      <div class="summary__item">
        <div class="summary__label">收款合计</div>
        <div class="summary__value summary__value--income mido-mono">{{ fmtAmount(summary.totalPayment) }}</div>
      </div>
      <div class="summary__item">
        <div class="summary__label">退款合计</div>
        <div class="summary__value summary__value--refund mido-mono">{{ fmtAmount(summary.totalRefund) }}</div>
      </div>
      <div class="summary__item">
        <div class="summary__label">净收入</div>
        <div class="summary__value summary__value--net mido-mono">{{ fmtAmount(summary.net) }}</div>
      </div>
      <div class="summary__item">
        <div class="summary__label">记录数</div>
        <div class="summary__value mido-mono">{{ summary.count || 0 }}</div>
      </div>
    </div>

    <div class="bar bar--filter">
      <el-select v-model="query.tenantId" placeholder="全部租户" clearable filterable class="bar__filter" @change="reload">
        <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="全部类型" clearable class="bar__filter" @change="reload">
        <el-option v-for="t in REVENUE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="租户" min-width="140">
        <template #default="{ row }">{{ row.tenantName || '—' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'refund' ? 'warning' : 'success'" effect="plain" size="small">
            {{ REVENUE_TYPE[row.type] || row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="150" align="right">
        <template #default="{ row }"><span class="mido-mono">{{ row.currency || 'CNY' }} {{ fmtAmount(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column prop="contractNo" label="合同号" min-width="140">
        <template #default="{ row }">{{ row.contractNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="发生日期" width="140">
        <template #default="{ row }">{{ row.occurredDate || '—' }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160">
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!ops.hasPerm('platform:revenue:manage')" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!ops.hasPerm('platform:revenue:manage')" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无收入记录" /></template>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="reload"
      />
    </div>
    </template>

    <!-- 新增 / 编辑（右抽屉）-->
    <el-drawer v-model="drawer" :title="editing ? '编辑记录' : '新增记录'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="90">
        <el-form-item label="租户" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="选择租户" filterable style="width: 100%">
            <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
            <el-option v-for="t in REVENUE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-select v-model="form.currency" style="width: 100%">
            <el-option v-for="c in CURRENCY_OPTIONS" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同号"><el-input v-model="form.contractNo" /></el-form-item>
        <el-form-item label="发生日期" prop="occurredDate">
          <el-date-picker v-model="form.occurredDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import ErrorState from '@/components/ErrorState.vue'
import { revenueApi, tenantApi, REVENUE_TYPE_OPTIONS, REVENUE_TYPE } from '@/api/ops'
import { useOpsUserStore } from '@/store/opsUser'
import { exportCsv } from '@/utils/exportCsv'

const ops = useOpsUserStore()
const CURRENCY_OPTIONS = ['CNY', 'USD', 'HKD', 'EUR']

function doExport() {
  const cols = [
    { key: 'tenantName', title: '租户' },
    { key: 'typeLabel', title: '类型' },
    { key: 'amount', title: '金额' },
    { key: 'currency', title: '币种' },
    { key: 'contractNo', title: '合同号' },
    { key: 'occurredDate', title: '发生日期' },
    { key: 'remark', title: '备注' },
  ]
  const data = rows.value.map((r) => ({ ...r, typeLabel: REVENUE_TYPE[r.type] || r.type }))
  exportCsv('收入台账', cols, data)
}

const loading = ref(false)
const loadError = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ tenantId: '', type: '', page: 1, size: 10 })
const summary = reactive({ totalPayment: 0, totalRefund: 0, net: 0, count: 0 })
const tenants = ref([])

function fmtAmount(v) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const res = await revenueApi.query({
      tenantId: query.tenantId || undefined,
      type: query.type || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.list || []
    total.value = Number(res.total || 0)
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}
async function loadSummary() {
  const res = await revenueApi.summary(query.tenantId)
  Object.assign(summary, {
    totalPayment: res?.totalPayment || 0,
    totalRefund: res?.totalRefund || 0,
    net: res?.net || 0,
    count: res?.count || 0,
  })
}
function reload() {
  query.page = 1
  load()
  loadSummary()
}

/* ===== 新增 / 编辑 ===== */
const drawer = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, tenantId: '', type: 'payment', amount: 0, contractNo: '', occurredDate: '', remark: '' })
const rules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  occurredDate: [{ required: true, message: '请选择发生日期', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, { id: null, tenantId: '', type: 'payment', amount: 0, currency: 'CNY', contractNo: '', occurredDate: '', remark: '' })
}
function openCreate() {
  editing.value = false
  resetForm()
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, tenantId: row.tenantId, type: row.type, amount: Number(row.amount || 0),
    currency: row.currency || 'CNY',
    contractNo: row.contractNo || '', occurredDate: row.occurredDate || '', remark: row.remark || '',
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      tenantId: form.tenantId, type: form.type, amount: form.amount, currency: form.currency,
      contractNo: form.contractNo || undefined, occurredDate: form.occurredDate,
      remark: form.remark || undefined,
    }
    if (editing.value) {
      await revenueApi.update(form.id, payload)
    } else {
      await revenueApi.create(payload)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
    loadSummary()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除该${row.type === 'refund' ? '退款' : '收款'}记录？`, '提示', { type: 'warning' })
  await revenueApi.remove(row.id)
  ElMessage.success('已删除')
  load()
  loadSummary()
}

onMounted(async () => {
  load()
  loadSummary()
  try {
    const res = await tenantApi.query({ page: 1, size: 200 })
    tenants.value = res.list || []
  } catch {
    tenants.value = []
  }
})
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.bar__right {
  display: flex;
  gap: var(--mido-space-2);
}
.bar--filter {
  justify-content: flex-start;
  gap: var(--mido-space-2);
}
.bar__filter {
  width: var(--mido-admin-nav-width);
}
.summary {
  display: flex;
  gap: var(--mido-space-4);
  margin-bottom: var(--mido-space-5);
}
.summary__item {
  flex: 1;
  padding: var(--mido-space-4);
  background-color: var(--el-bg-color-page);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius);
}
.summary__label {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-secondary);
  margin-bottom: var(--mido-space-2);
}
.summary__value {
  font-size: var(--mido-font-size-h1);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
.summary__value--income {
  color: var(--el-color-success);
}
.summary__value--refund {
  color: var(--el-color-danger);
}
.summary__value--net {
  color: var(--el-color-primary);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
</style>
