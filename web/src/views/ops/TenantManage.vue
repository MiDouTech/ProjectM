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
        <el-button :icon="Refresh" :loading="snapshotLoading" @click="refreshUsage">刷新用量</el-button>
        <el-button type="primary" :icon="Plus" :disabled="!ops.hasPerm('platform:tenant:manage')" @click="openCreate">开通租户</el-button>
      </div>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-skeleton v-else-if="loading && !rows.length" :rows="6" animated :throttle="300" />
    <template v-else>
    <div v-if="selectedIds.length" class="batchbar">
      <span class="batchbar__info">已选 {{ selectedIds.length }} 个租户</span>
      <el-button size="small" type="success" plain :disabled="!ops.hasPerm('platform:tenant:manage')"
        @click="batchStatus('active')">批量启用</el-button>
      <el-button size="small" type="warning" plain :disabled="!ops.hasPerm('platform:tenant:manage')"
        @click="batchStatus('suspended')">批量停用</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" />
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
          <el-button link type="primary" :disabled="!ops.hasPerm('platform:tenant:manage')" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'suspended'" link type="success" :disabled="!ops.hasPerm('platform:tenant:manage')" @click="changeStatus(row, 'active')">启用</el-button>
          <el-button v-else-if="row.status !== 'closed'" link type="warning" :disabled="!ops.hasPerm('platform:tenant:manage')" @click="changeStatus(row, 'suspended')">停用</el-button>
          <el-button v-if="row.status !== 'closed'" link type="danger" :disabled="!ops.hasPerm('platform:tenant:manage')" @click="changeStatus(row, 'closed')">注销</el-button>
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
    </template>

    <!-- 开通 / 编辑（右抽屉） -->
    <el-drawer v-model="formDrawer" :title="editing ? '编辑租户' : '开通租户'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="90">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!editing" label="编码" prop="code">
          <el-input v-model="form.code" placeholder="2-32 位小写字母/数字/连字符，不以连字符开头" />
        </el-form-item>
        <el-form-item v-if="!editing" label="管理员账号">
          <el-input v-model="form.adminUsername" placeholder="留空默认 admin" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码">
          <el-input v-model="form.adminPassword" placeholder="留空将生成默认初始密码，请提示客户首登后立即修改" />
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
          <div class="detail__actions">
            <el-button type="primary" plain :icon="Switch" :disabled="!ops.hasPerm('platform:tenant:impersonate')" @click="impersonate">模拟登录</el-button>
          </div>

          <el-tabs class="detail__tabs">
            <el-tab-pane label="基础">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="编码"><span class="mido-mono">{{ detail.code }}</span></el-descriptions-item>
                <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <StatusTag :status="detail.status" :label="tenantLabel(detail.status)" />
                </el-descriptions-item>
                <el-descriptions-item label="行业">{{ detail.industry || '—' }}</el-descriptions-item>
                <el-descriptions-item label="联系人">{{ detail.contactName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="联系电话"><span class="mido-mono">{{ detail.contactPhone || '—' }}</span></el-descriptions-item>
                <el-descriptions-item label="联系邮箱">{{ detail.contactEmail || '—' }}</el-descriptions-item>
                <el-descriptions-item label="来源">{{ detail.source || '—' }}</el-descriptions-item>
                <el-descriptions-item label="激活时间">{{ detail.activatedAt || '—' }}</el-descriptions-item>
                <el-descriptions-item label="到期时间">{{ detail.expireAt || '不限期' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ detail.createTime || '—' }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane label="订阅与配额">
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
                  <el-table-column label="上限" align="right">
                    <template #default="{ row }"><span class="mido-mono">{{ row.limitValue === -1 ? '不限' : row.limitValue }}</span></template>
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
                  <el-button type="primary" :loading="subSaving"
                    :disabled="!subForm.planId || !ops.hasPerm('platform:subscription:manage')" @click="saveSubscription">
                    绑定 / 续期
                  </el-button>
                </el-form>
              </div>
            </el-tab-pane>

            <el-tab-pane label="用量">
              <el-table v-if="usage.length" :data="usage" size="small" border>
                <el-table-column label="资源">
                  <template #default="{ row }">{{ resourceLabel(row.resource) }}</template>
                </el-table-column>
                <el-table-column label="已用" width="90" align="right">
                  <template #default="{ row }"><span class="mido-mono">{{ row.used }}</span></template>
                </el-table-column>
                <el-table-column label="上限" width="90" align="right">
                  <template #default="{ row }"><span class="mido-mono">{{ row.limit === -1 ? '不限' : row.limit }}</span></template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <StatusTag v-if="row.exceeded" status="逾期" label="超限" />
                    <StatusTag v-else status="达标" label="正常" />
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无用量数据" :image-size="60" />
            </el-tab-pane>

            <el-tab-pane label="导出与注销">
              <div class="detail__block">
                <div class="detail__sub-title detail__sub-title--bar">
                  <span>数据导出</span>
                  <el-button type="primary" plain size="small" :icon="Download"
                    :disabled="!ops.hasPerm('platform:tenant:manage')"
                    :loading="exportRequesting" @click="requestExport">发起导出</el-button>
                </div>
                <el-table v-if="exports.length" :data="exports" size="small" border>
                  <el-table-column label="创建时间" min-width="160">
                    <template #default="{ row }">{{ row.createTime || '—' }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <StatusTag :status="row.status" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120">
                    <template #default="{ row }">
                      <el-button v-if="row.status === 'done' && row.fileReady" link type="primary"
                        @click="downloadExport(row)">下载</el-button>
                      <el-tooltip v-else-if="row.status === 'failed' && row.error"
                        :content="row.error" placement="top">
                        <span class="detail__error">查看错误</span>
                      </el-tooltip>
                      <span v-else>—</span>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-else description="暂无导出任务" :image-size="60" />
              </div>

              <div class="detail__block">
                <div class="detail__sub-title">注销合规</div>
                <el-alert v-if="detail.purgeScheduledAt" type="error" :closable="false" show-icon
                  :title="`已注销，计划清除时间：${detail.purgeScheduledAt}`"
                  description="该租户处于注销宽限期，到期后数据将被物理清除。" />
                <div class="detail__deletion">
                  <el-button v-if="detail.purgeScheduledAt" type="warning"
                    :disabled="!ops.hasPerm('platform:tenant:manage')"
                    :loading="deletionLoading" @click="cancelDeletion">取消注销</el-button>
                  <el-button v-else-if="String(detail.id) !== '1'" type="danger"
                    :disabled="!ops.hasPerm('platform:tenant:manage')"
                    :loading="deletionLoading" @click="requestDeletion">注销租户</el-button>
                  <span v-else class="detail__hint">自用租户不可注销</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Switch, Download } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import { tenantApi, planApi, usageApi, TENANT_STATUS, QUOTA_RESOURCE } from '@/api/ops'
import { TOKEN_KEY } from '@/store/user'
import { useOpsUserStore } from '@/store/opsUser'

const ops = useOpsUserStore()
const loading = ref(false)
const loadError = ref(false)
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
  loadError.value = false
  try {
    const res = await tenantApi.query({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
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
function reload() {
  query.page = 1
  load()
}

/* ===== 批量操作 ===== */
const selectedIds = ref([])
function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}
async function batchStatus(status) {
  const label = status === 'active' ? '启用' : '停用'
  await ElMessageBox.confirm(`确认批量${label}选中的 ${selectedIds.value.length} 个租户？`, '批量操作', { type: 'warning' })
  const n = await tenantApi.batchStatus({ ids: selectedIds.value, status })
  ElMessage.success(`已${label} ${n} 个租户`)
  selectedIds.value = []
  load()
}

/* ===== 开通 / 编辑 ===== */
const formDrawer = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', code: '', industry: '', contactName: '', contactPhone: '', contactEmail: '', remark: '', adminUsername: '', adminPassword: '' })
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[a-z0-9][a-z0-9-]{1,31}$/, message: '2-32 位小写字母/数字/连字符，不以连字符开头', trigger: 'blur' },
  ],
}

function resetForm() {
  Object.assign(form, { id: null, name: '', code: '', industry: '', contactName: '', contactPhone: '', contactEmail: '', remark: '', adminUsername: '', adminPassword: '' })
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
      await tenantApi.create({ ...payload, code: form.code, adminUsername: form.adminUsername, adminPassword: form.adminPassword })
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
const usage = ref([])
const subForm = reactive({ planId: '', startAt: '', expireAt: '', remark: '' })
const subSaving = ref(false)
const exports = ref([])
const exportRequesting = ref(false)
const deletionLoading = ref(false)
let currentId = null

async function openDetail(row) {
  currentId = row.id
  detailDrawer.value = true
  detailLoading.value = true
  Object.keys(detail).forEach((k) => delete detail[k])
  Object.assign(subForm, { planId: '', startAt: '', expireAt: '', remark: '' })
  usage.value = []
  exports.value = []
  try {
    if (!plans.value.length) plans.value = await planApi.list()
    const [info, usageRows, exportRows] = await Promise.all([
      tenantApi.get(row.id),
      tenantApi.usage(row.id),
      tenantApi.exports(row.id),
    ])
    Object.assign(detail, info)
    usage.value = usageRows || []
    exports.value = exportRows || []
    if (detail.subscription?.planId) subForm.planId = detail.subscription.planId
  } finally {
    detailLoading.value = false
  }
}

/* ===== 数据导出 ===== */
async function loadExports() {
  exports.value = (await tenantApi.exports(currentId)) || []
}
async function requestExport() {
  exportRequesting.value = true
  try {
    await tenantApi.requestExport(currentId)
    ElMessage.success('已发起导出，任务处理中')
    await loadExports()
  } finally {
    exportRequesting.value = false
  }
}
async function downloadExport(row) {
  const res = await tenantApi.exportDownload(currentId, row.id)
  if (res?.url) {
    window.open(res.url, '_blank')
  } else {
    ElMessage.warning('下载链接暂不可用')
  }
}

/* ===== 注销合规（高危，二次确认）===== */
async function refreshDetail() {
  Object.assign(detail, await tenantApi.get(currentId))
}
async function requestDeletion() {
  try {
    await ElMessageBox.confirm(
      `这是高危操作：将停用租户「${detail.name}」，并在宽限期（默认 30 天）结束后【物理清除】其全部数据，不可恢复。确认发起注销？`,
      '注销租户',
      { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
    )
  } catch {
    return
  }
  deletionLoading.value = true
  try {
    await tenantApi.requestDeletion(currentId)
    ElMessage.success('已发起注销，进入宽限期')
    await refreshDetail()
    load()
  } finally {
    deletionLoading.value = false
  }
}
async function cancelDeletion() {
  try {
    await ElMessageBox.confirm(
      `确认取消租户「${detail.name}」的注销流程？取消后将不再物理清除数据。`,
      '取消注销',
      { type: 'warning', confirmButtonText: '确认取消注销', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  deletionLoading.value = true
  try {
    await tenantApi.cancelDeletion(currentId)
    ElMessage.success('已取消注销')
    await refreshDetail()
    load()
  } finally {
    deletionLoading.value = false
  }
}

/* ===== 模拟登录（高敏感，二次确认 + 审计）===== */
async function impersonate() {
  try {
    await ElMessageBox.confirm(
      '这是高敏感操作：将以该租户用户身份进入租户应用，操作会被审计。确认继续？',
      '模拟登录',
      { type: 'warning', confirmButtonText: '确认进入', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const res = await tenantApi.impersonate(currentId)
  // 短时租户令牌写入租户应用 token（与运营台 token 隔离），再新标签页打开租户应用
  localStorage.setItem(TOKEN_KEY, res.token)
  window.open('/', '_blank')
  ElMessage.success('已生成模拟登录令牌，已在新标签页打开租户应用')
}

/* ===== 刷新用量（手动触发全量快照）===== */
const snapshotLoading = ref(false)
async function refreshUsage() {
  snapshotLoading.value = true
  try {
    const count = await usageApi.snapshot()
    ElMessage.success(`用量快照已刷新，共处理 ${count} 个租户`)
    // 若当前正打开详情，重拉该租户用量
    if (detailDrawer.value && currentId) {
      usage.value = (await tenantApi.usage(currentId)) || []
    }
  } finally {
    snapshotLoading.value = false
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
.batchbar {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2) var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
  background-color: var(--el-fill-color-light);
  border-radius: var(--mido-radius-md);
}
.batchbar__info {
  margin-right: auto;
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-regular);
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
.detail__actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--mido-space-4);
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
.detail__sub-title--bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.detail__error {
  color: var(--el-color-danger);
  cursor: pointer;
}
.detail__deletion {
  margin-top: var(--mido-space-3);
}
.detail__hint {
  color: var(--el-text-color-secondary);
}
</style>
