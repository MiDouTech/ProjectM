<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">租户管理</h2>
      <div class="bar__right">
        <el-input v-model="query.keyword" placeholder="搜索编码 / 名称" clearable class="bar__search"
          @keyup.enter="reload" @clear="reload" />
        <el-select v-model="query.status" placeholder="全部状态" clearable class="bar__filter" @change="reload">
          <el-option v-for="s in TENANT_STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">开通租户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status" :label="tenantLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="套餐" width="140">
        <template #default="{ row }">{{ row.planName || '—' }}</template>
      </el-table-column>
      <el-table-column label="到期" width="180">
        <template #default="{ row }">{{ row.expireAt || '不限期' }}</template>
      </el-table-column>
      <el-table-column label="联系" min-width="160">
        <template #default="{ row }">
          <span v-if="row.contactName || row.contactPhone">
            {{ row.contactName || '' }} {{ row.contactPhone || '' }}
          </span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'suspended'" link type="success" @click="changeStatus(row, 'active')">启用</el-button>
          <el-button v-else-if="row.status !== 'closed'" link type="warning" @click="changeStatus(row, 'suspended')">停用</el-button>
          <el-button v-if="row.status !== 'closed'" link type="danger" @click="changeStatus(row, 'closed')">注销</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无租户，点击开通" /></template>
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

    <!-- 开通 / 编辑（右抽屉） -->
    <el-drawer v-model="formDrawer" :title="editing ? '编辑租户' : '开通租户'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="90">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!editing" label="编码" prop="code">
          <el-input v-model="form.code" placeholder="2-32 位小写字母/数字/连字符，不以连字符开头" />
        </el-form-item>
        <el-form-item label="行业"><el-input v-model="form.industry" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="联系邮箱"><el-input v-model="form.contactEmail" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 详情（右抽屉）-->
    <el-drawer v-model="detailDrawer" title="租户详情" size="var(--mido-drawer-width)">
      <div v-loading="detailLoading" class="detail">
        <template v-if="detail.id">
          <el-descriptions title="基础信息" :column="1" border>
            <el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="detail.status" :label="tenantLabel(detail.status)" />
            </el-descriptions-item>
            <el-descriptions-item label="行业">{{ detail.industry || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ detail.contactName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.contactPhone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系邮箱">{{ detail.contactEmail || '—' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ detail.source || '—' }}</el-descriptions-item>
            <el-descriptions-item label="激活时间">{{ detail.activatedAt || '—' }}</el-descriptions-item>
            <el-descriptions-item label="到期时间">{{ detail.expireAt || '不限期' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions class="detail__block" title="当前订阅" :column="1" border>
            <template v-if="detail.subscription">
              <el-descriptions-item label="套餐">{{ detail.subscription.planName }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <StatusTag :status="detail.subscription.status" />
              </el-descriptions-item>
              <el-descriptions-item label="开始">{{ detail.subscription.startAt || '—' }}</el-descriptions-item>
              <el-descriptions-item label="到期">{{ detail.subscription.expireAt || '不限期' }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ detail.subscription.remark || '—' }}</el-descriptions-item>
            </template>
            <el-descriptions-item v-else label="订阅">暂无订阅</el-descriptions-item>
          </el-descriptions>

          <div class="detail__block">
            <div class="detail__sub-title">配额</div>
            <el-table v-if="detail.quotas && detail.quotas.length" :data="detail.quotas" size="small" border>
              <el-table-column label="资源">
                <template #default="{ row }">{{ resourceLabel(row.resource) }}</template>
              </el-table-column>
              <el-table-column label="上限">
                <template #default="{ row }">{{ row.limitValue === -1 ? '不限' : row.limitValue }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无配额" :image-size="60" />
          </div>

          <div class="detail__block">
            <div class="detail__sub-title">绑定 / 续期套餐</div>
            <el-form :model="subForm" :label-width="80">
              <el-form-item label="套餐">
                <el-select v-model="subForm.planId" placeholder="选择套餐" style="width: 100%">
                  <el-option v-for="p in plans" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="开始日期">
                <el-date-picker v-model="subForm.startAt" type="date" value-format="YYYY-MM-DD"
                  placeholder="可空，默认即时生效" style="width: 100%" />
              </el-form-item>
              <el-form-item label="到期日期">
                <el-date-picker v-model="subForm.expireAt" type="date" value-format="YYYY-MM-DD"
                  placeholder="可空，留空=不限期" style="width: 100%" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="subForm.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="primary" :loading="subSaving" :disabled="!subForm.planId" @click="saveSubscription">
                绑定 / 续期
              </el-button>
            </el-form>
          </div>
        </template>
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { tenantApi, planApi, TENANT_STATUS, QUOTA_RESOURCE } from '@/api/ops'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

const plans = ref([])

function tenantLabel(status) {
  return TENANT_STATUS.find((s) => s.value === status)?.label || ''
}
function resourceLabel(res) {
  return QUOTA_RESOURCE.find((r) => r.value === res)?.label || res
}

async function load() {
  loading.value = true
  try {
    const res = await tenantApi.query({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.list || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}
function reload() {
  query.page = 1
  load()
}

/* ===== 开通 / 编辑 ===== */
const formDrawer = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', code: '', industry: '', contactName: '', contactPhone: '', contactEmail: '', remark: '' })
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[a-z0-9][a-z0-9-]{1,31}$/, message: '2-32 位小写字母/数字/连字符，不以连字符开头', trigger: 'blur' },
  ],
}

function resetForm() {
  Object.assign(form, { id: null, name: '', code: '', industry: '', contactName: '', contactPhone: '', contactEmail: '', remark: '' })
}
function openCreate() {
  editing.value = false
  resetForm()
  formDrawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, name: row.name, code: row.code, industry: row.industry || '',
    contactName: row.contactName || '', contactPhone: row.contactPhone || '',
    contactEmail: row.contactEmail || '', remark: row.remark || '',
  })
  formDrawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, industry: form.industry, contactName: form.contactName,
      contactPhone: form.contactPhone, contactEmail: form.contactEmail, remark: form.remark,
    }
    if (editing.value) {
      await tenantApi.update(form.id, payload)
    } else {
      await tenantApi.create({ ...payload, code: form.code })
    }
    ElMessage.success('保存成功')
    formDrawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

/* ===== 状态流转 ===== */
const STATUS_ACTION_LABEL = { active: '启用', suspended: '停用', closed: '注销' }
async function changeStatus(row, status) {
  const actLabel = STATUS_ACTION_LABEL[status]
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(`确认对租户「${row.name}」执行「${actLabel}」？可填写原因。`, '提示', {
      type: 'warning',
      inputPlaceholder: '原因（可选）',
      inputValue: '',
    })
    reason = r.value || ''
  } catch {
    return
  }
  await tenantApi.changeStatus(row.id, { status, reason })
  ElMessage.success('已更新状态')
  load()
}

/* ===== 详情 ===== */
const detailDrawer = ref(false)
const detailLoading = ref(false)
const detail = reactive({})
const subForm = reactive({ planId: '', startAt: '', expireAt: '', remark: '' })
const subSaving = ref(false)
let currentId = null

async function openDetail(row) {
  currentId = row.id
  detailDrawer.value = true
  detailLoading.value = true
  Object.keys(detail).forEach((k) => delete detail[k])
  Object.assign(subForm, { planId: '', startAt: '', expireAt: '', remark: '' })
  try {
    if (!plans.value.length) plans.value = await planApi.list()
    Object.assign(detail, await tenantApi.get(row.id))
    if (detail.subscription?.planId) subForm.planId = detail.subscription.planId
  } finally {
    detailLoading.value = false
  }
}
async function saveSubscription() {
  subSaving.value = true
  try {
    await tenantApi.bindSubscription(currentId, {
      planId: subForm.planId,
      startAt: subForm.startAt || undefined,
      expireAt: subForm.expireAt || undefined,
      remark: subForm.remark || undefined,
    })
    ElMessage.success('已绑定 / 续期套餐')
    // 刷新详情与列表
    Object.assign(detail, await tenantApi.get(currentId))
    load()
  } finally {
    subSaving.value = false
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
.bar__right {
  display: flex;
  gap: var(--mido-space-2);
}
.bar__search {
  width: var(--mido-admin-nav-width);
}
.bar__filter {
  width: var(--mido-admin-nav-width);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
.detail__block {
  margin-top: var(--mido-space-5);
}
.detail__sub-title {
  margin-bottom: var(--mido-space-3);
  font-size: var(--mido-font-size-h2);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
</style>
