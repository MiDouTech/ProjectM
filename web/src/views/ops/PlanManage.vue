<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">套餐管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建套餐</el-button>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-table v-else v-loading="loading" :data="rows" stripe>
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="价格" width="120">
        <template #default="{ row }">{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="计费周期" width="110">
        <template #default="{ row }">{{ cycleLabel(row.billingCycle) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="配额项" min-width="160">
        <template #default="{ row }">{{ quotaSummary(row.quotas) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openFeatures(row)">功能开关</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无套餐，点击新建" /></template>
    </el-table>

    <!-- 新建 / 编辑（右抽屉）-->
    <el-drawer v-model="drawer" :title="editing ? '编辑套餐' : '新建套餐'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="90">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="editing" placeholder="套餐编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="计费周期" prop="billingCycle">
          <el-select v-model="form.billingCycle" placeholder="选择计费周期" style="width: 100%">
            <el-option v-for="c in BILLING_CYCLE" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ENABLE_STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="配额项">
          <div class="quotas">
            <div v-for="(q, i) in form.quotas" :key="i" class="quotas__row">
              <el-select v-model="q.resource" placeholder="资源" class="quotas__res">
                <el-option v-for="r in QUOTA_RESOURCE" :key="r.value" :label="r.label" :value="r.value" />
              </el-select>
              <el-input-number v-model="q.limitValue" :min="-1" :step="1" class="quotas__limit" />
              <el-button link type="danger" :icon="Delete" @click="form.quotas.splice(i, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="addQuota">添加配额项（-1=不限）</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 功能开关（右抽屉）-->
    <el-drawer v-model="featuresDrawer" title="功能开关" size="var(--mido-drawer-width)">
      <div v-loading="featuresLoading" class="features">
        <div class="features__hint">为套餐「{{ featurePlanName }}」配置可用功能模块。</div>
        <div v-for="f in featureList" :key="f.featureCode" class="features__row">
          <span class="features__name">{{ featureLabel(f.featureCode) }}</span>
          <el-switch v-model="f.enabled" />
        </div>
      </div>
      <template #footer>
        <el-button @click="featuresDrawer = false">取消</el-button>
        <el-button type="primary" :loading="featuresSaving" @click="saveFeatures">保存</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import { planApi, BILLING_CYCLE, ENABLE_STATUS, QUOTA_RESOURCE, FEATURE_LABELS } from '@/api/ops'

const loading = ref(false)
const loadError = ref(false)
const rows = ref([])

function cycleLabel(c) {
  return BILLING_CYCLE.find((x) => x.value === c)?.label || c
}
function resourceLabel(res) {
  return QUOTA_RESOURCE.find((r) => r.value === res)?.label || res
}
function quotaSummary(quotas) {
  if (!quotas || !quotas.length) return '—'
  return quotas.map((q) => `${resourceLabel(q.resource)}:${q.limitValue === -1 ? '不限' : q.limitValue}`).join('，')
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    rows.value = await planApi.list()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const drawer = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, code: '', name: '', price: 0, billingCycle: 'monthly', status: 'active', sort: 0, remark: '', quotas: [] })
const rules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  billingCycle: [{ required: true, message: '请选择计费周期', trigger: 'change' }],
}

function addQuota() {
  form.quotas.push({ resource: 'user', limitValue: -1 })
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, code: '', name: '', price: 0, billingCycle: 'monthly', status: 'active', sort: 0, remark: '', quotas: [] })
  drawer.value = true
}
async function openEdit(row) {
  editing.value = true
  // 拉取完整套餐（含配额）
  const full = await planApi.get(row.id)
  Object.assign(form, {
    id: full.id, code: full.code, name: full.name, price: full.price,
    billingCycle: full.billingCycle, status: full.status, sort: full.sort || 0,
    remark: full.remark || '',
    quotas: (full.quotas || []).map((q) => ({ resource: q.resource, limitValue: q.limitValue })),
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      code: form.code, name: form.name, price: form.price, billingCycle: form.billingCycle,
      status: form.status, sort: form.sort, remark: form.remark,
      quotas: form.quotas.map((q) => ({ resource: q.resource, limitValue: q.limitValue })),
    }
    if (editing.value) {
      await planApi.update(form.id, payload)
    } else {
      await planApi.create(payload)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？被订阅的套餐无法删除。`, '提示', { type: 'warning' })
  await planApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

/* ===== 功能开关（独立右抽屉，单 PUT）===== */
const FEATURE_CODES = Object.keys(FEATURE_LABELS)
const featuresDrawer = ref(false)
const featuresLoading = ref(false)
const featuresSaving = ref(false)
const featureList = ref([])
const featurePlanName = ref('')
let featurePlanId = null

function featureLabel(code) {
  return FEATURE_LABELS[code] || code
}
async function openFeatures(row) {
  featurePlanId = row.id
  featurePlanName.value = row.name
  featuresDrawer.value = true
  featuresLoading.value = true
  // 先用全量功能码占位（默认关闭），再用后端返回的 enabled 覆盖
  featureList.value = FEATURE_CODES.map((code) => ({ featureCode: code, enabled: false }))
  try {
    const res = (await planApi.features(row.id)) || []
    const enabledMap = {}
    res.forEach((f) => { enabledMap[f.featureCode] = !!f.enabled })
    featureList.value = FEATURE_CODES.map((code) => ({
      featureCode: code,
      enabled: code in enabledMap ? enabledMap[code] : false,
    }))
  } finally {
    featuresLoading.value = false
  }
}
async function saveFeatures() {
  featuresSaving.value = true
  try {
    await planApi.saveFeatures(featurePlanId, {
      features: featureList.value.map((f) => ({ featureCode: f.featureCode, enabled: f.enabled })),
    })
    ElMessage.success('功能开关已保存')
    featuresDrawer.value = false
  } finally {
    featuresSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.quotas {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
}
.quotas__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.quotas__res {
  width: var(--mido-admin-nav-width);
}
.quotas__limit {
  width: var(--mido-admin-nav-width);
}
.features {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
}
.features__hint {
  margin-bottom: var(--mido-space-2);
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-secondary);
}
.features__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--mido-space-2) var(--mido-space-3);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius);
}
.features__name {
  color: var(--el-text-color-primary);
}
</style>
