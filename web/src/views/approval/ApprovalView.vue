<template>
  <div class="mido-page">
    <WorkspaceShell module="approval">
      <template #actions>
        <div v-show="activeTab === 'all'" class="apv__lookup">
          <el-input v-model="lookupId" placeholder="输入审批实例 ID" class="apv__input"
            :prefix-icon="Search" @keyup.enter="open" />
          <el-button type="primary" :disabled="!lookupId" @click="open">打开</el-button>
        </div>
      </template>
    </WorkspaceShell>

    <el-tabs v-model="activeTab" class="apv__tabs apv__tabs--headless">
      <!-- 全部：与我相关的审批（我发起 ∪ 待我处理 ∪ 我已处理）单列表 + 筛选，点行右抽屉看详情 -->
      <el-tab-pane label="全部" name="all">
        <!-- 顶部操作条（对齐项目模块：视图切换 + 角色/类型/状态 + 搜索） -->
        <div class="apv__bar">
          <ViewSwitcher v-model="view" :views="VIEWS" />
          <div class="apv__bar-right">
            <el-select v-model="roleFilter" placeholder="全部角色" class="apv__quick">
              <el-option v-for="r in ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
            </el-select>
            <el-select v-model="bizFilter" clearable placeholder="全部类型" class="apv__quick">
              <el-option v-for="b in bizFilterOptions" :key="b.value" :label="b.label" :value="b.value" />
            </el-select>
            <el-select v-model="statusFilter" clearable placeholder="全部状态" class="apv__quick">
              <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索标题 / 实例 ID" clearable class="apv__search"
              :prefix-icon="Search" />
          </div>
        </div>

        <el-card shadow="never" v-loading="allLoading">
          <!-- 列表视图：表头可排序（提交时间/状态，客户端排序后再分页） -->
          <el-table v-if="view === 'list'" :data="pagedAll" stripe class="is-clickable"
            :default-sort="{ prop: sortProp, order: sortOrder }" @sort-change="onSortChange" @row-click="openDetail">
            <el-table-column label="审批对象" min-width="220">
              <template #default="{ row }">
                <div class="apv__obj">{{ row.title || bizTypeLabel(row.bizType) }}</div>
                <div class="mido-text-secondary">{{ bizTypeLabel(row.bizType) }} · #{{ row.instanceId }}</div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">{{ bizTypeLabel(row.bizType) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110" prop="status" sortable="custom">
              <template #default="{ row }"><StatusTag :status="statusZh(row.status)" /></template>
            </el-table-column>
            <el-table-column label="我的角色" width="150">
              <template #default="{ row }">
                <div class="apv__roles">
                  <el-tag v-if="row.mineToAct" size="small" type="warning" disable-transitions>待我审批</el-tag>
                  <el-tag v-if="row.iInitiated" size="small" type="primary" disable-transitions>我发起</el-tag>
                  <el-tag v-if="row.processedByMe" size="small" type="info" disable-transitions>我已处理</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="申请人" width="110">
              <template #default="{ row }">{{ userName(members, row.applicantId) }}</template>
            </el-table-column>
            <el-table-column label="提交时间" width="140" prop="submittedAt" sortable="custom">
              <template #default="{ row }">{{ fmtDate(row.submittedAt) }}</template>
            </el-table-column>
            <template #empty>
              <EmptyState :description="all.length ? '无符合筛选的审批' : '暂无与我相关的审批'" :image-size="60" />
            </template>
          </el-table>

          <!-- 卡片视图 -->
          <div v-else class="apv__cards">
            <div v-for="row in pagedAll" :key="row.instanceId" class="apv__card" @click="openDetail(row)">
              <div class="apv__card-top">
                <StatusTag :status="statusZh(row.status)" />
                <span class="mido-mono mido-text-secondary">#{{ row.instanceId }}</span>
              </div>
              <div class="apv__card-title">{{ row.title || bizTypeLabel(row.bizType) }}</div>
              <div class="mido-text-secondary">{{ bizTypeLabel(row.bizType) }}</div>
              <div class="apv__roles apv__card-roles">
                <el-tag v-if="row.mineToAct" size="small" type="warning" disable-transitions>待我审批</el-tag>
                <el-tag v-if="row.iInitiated" size="small" type="primary" disable-transitions>我发起</el-tag>
                <el-tag v-if="row.processedByMe" size="small" type="info" disable-transitions>我已处理</el-tag>
              </div>
              <div class="apv__card-foot mido-text-secondary">
                <span>{{ userName(members, row.applicantId) }}</span>
                <span>{{ fmtDate(row.submittedAt) }}</span>
              </div>
            </div>
            <EmptyState v-if="!pagedAll.length"
              :description="all.length ? '无符合筛选的审批' : '暂无与我相关的审批'" :image-size="60" />
          </div>

          <div class="apv__pager">
            <el-pagination layout="total, prev, pager, next" :total="total"
              :current-page="page" :page-size="size" @current-change="(p) => (page = p)" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 变更台账：复用变更中心组件（embedded 隐藏自身标题）；受 change 功能码门控 -->
      <el-tab-pane v-if="showChange" label="变更台账" name="change" lazy>
        <ChangeCenter embedded />
      </el-tab-pane>
    </el-tabs>

    <!-- 详情右抽屉：进度 + 业务卡 + 审批动作（design-system §4 详情走右抽屉） -->
    <el-drawer v-model="detailOpen" :title="detailTitle" size="var(--mido-drawer-width)">
      <template v-if="current">
        <div class="apv__head">
          <span class="mido-text-secondary">{{ bizTypeLabel(current.bizType) }} · 实例 #{{ current.id }} · 申请人：{{ userName(members, current.applicantId) }}</span>
        </div>

        <!-- 项目情况：让审批人看到立项关键信息再决策（立项审批专属） -->
        <el-descriptions v-if="project" class="apv__proj" :column="2" border size="small">
          <el-descriptions-item label="项目编号"><span class="mido-mono">{{ project.code || '—' }}</span></el-descriptions-item>
          <el-descriptions-item label="类型">{{ categoryLabel(project.category) }}<template v-if="project.subCategory"> / {{ project.subCategory }}</template></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="project.status" /></el-descriptions-item>
          <el-descriptions-item label="负责人">{{ userName(members, project.leaderId) }}</el-descriptions-item>
          <el-descriptions-item label="预算(元)"><span class="mido-mono">{{ fmtMoney(project.budget) }}</span></el-descriptions-item>
          <el-descriptions-item label="周期">{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="项目目标" :span="2">{{ current.formData?.objective || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="current.formData?.valueHypothesis" label="价值假设" :span="2">{{ current.formData.valueHypothesis }}</el-descriptions-item>
        </el-descriptions>

        <ApprovalSteps ref="stepsRef" :instance-id="current.id" class="apv__drawer-steps" />

        <!-- 待我处理：审批动作 -->
        <template v-if="canAct">
          <el-divider />
          <el-form :label-width="64">
            <el-form-item label="意见">
              <el-input v-model="comment" type="textarea" :rows="2" placeholder="审批意见（可选）" />
            </el-form-item>
          </el-form>
          <div class="apv__actions">
            <el-button plain :disabled="acting" @click="transferVisible = true">转交</el-button>
            <el-button type="danger" plain :loading="acting" @click="act('reject')">驳回</el-button>
            <el-button type="primary" :loading="acting" @click="act('approve')">通过</el-button>
          </div>
          <el-alert v-if="current.bizType === 'project_init'" class="apv__warn" type="warning" :closable="false"
            show-icon title="严肃提示" description="未通过审批，项目不得进入执行态。驳回将退回申请人修改。" />
        </template>

        <!-- 我发起且仍在审批中：可撤回 -->
        <div v-if="canWithdraw" class="apv__actions apv__withdraw">
          <el-button type="danger" plain :loading="withdrawing" @click="doWithdraw">撤回申请</el-button>
        </div>
      </template>
      <el-empty v-else description="加载中…" />
    </el-drawer>

    <!-- 转交：经统一 UserSelect 选择受让人 -->
    <el-dialog v-model="transferVisible" title="转交审批" width="420">
      <el-form label-width="92">
        <el-form-item label="受让人" required>
          <UserSelect v-model="transferTo" placeholder="选择受让人" />
        </el-form-item>
        <el-form-item label="转交说明">
          <el-input v-model="transferComment" type="textarea" :rows="2" placeholder="转交说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferring" :disabled="!transferTo" @click="doTransfer">确认转交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import ApprovalSteps from '@/components/ApprovalSteps.vue'
import UserSelect from '@/components/UserSelect.vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import EmptyState from '@/components/EmptyState.vue'
import ChangeCenter from '@/views/ChangeCenter.vue'
import { approvalApi, projectApi, PROJECT_CATEGORIES } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { userName } from '@/utils/display'
import { useUserStore } from '@/store/user'

// 审批中心 Tab：全部 / 变更台账（变更受 change 功能码门控，fail-open）
const userStore = useUserStore()
const showChange = computed(() => userStore.hasFeature('change'))
const activeTab = ref('all')

// bizType 字典：单一信息源，挂载时从后端 /approvals/biz-types 拉取（不再前端硬编码）
const bizTypes = ref([])
const bizFilterOptions = computed(() => (showChange.value
  ? bizTypes.value
  : bizTypes.value.filter((b) => b.value !== 'change')))
const bizTypeLabel = (t) => bizTypes.value.find((b) => b.value === t)?.label || t

// 角色 / 状态筛选项
const ROLE_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'toAct', label: '待我审批' },
  { value: 'initiated', label: '我发起的' },
  { value: 'processed', label: '我已处理' },
]
const STATUS_OPTIONS = [
  { value: 'pending', label: '审批中' },
  { value: 'approved', label: '已结案' },
  { value: 'rejected', label: '失败' },
  { value: 'withdrawn', label: '已撤回' },
]

// 视图：列表 / 卡片（数据量 ≤ MINE_LIMIT，分页与排序均在客户端完成）
const VIEWS = [
  { value: 'list', label: '列表' },
  { value: 'card', label: '卡片' },
]
const view = ref('list')
const page = ref(1)
const size = ref(20)
const sortProp = ref('submittedAt')
const sortOrder = ref('descending')

// 全部列表：一次拉取，角色/类型/状态/关键字均在客户端筛选（数据带角色标记，无需多次请求）
const all = ref([])
const allLoading = ref(false)
const roleFilter = ref('')
const bizFilter = ref('')
const statusFilter = ref('')
const keyword = ref('')
const filteredAll = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return all.value.filter((r) => {
    if (roleFilter.value === 'toAct' && !r.mineToAct) return false
    if (roleFilter.value === 'initiated' && !r.iInitiated) return false
    if (roleFilter.value === 'processed' && !r.processedByMe) return false
    if (bizFilter.value && r.bizType !== bizFilter.value) return false
    if (statusFilter.value && r.status !== statusFilter.value) return false
    if (kw && !(`${r.title || ''}`.toLowerCase().includes(kw) || String(r.instanceId).includes(kw))) return false
    return true
  })
})
const total = computed(() => filteredAll.value.length)
// 客户端排序：先按表头排序整份筛选结果，再切片分页（避免内置排序只作用于当页）
const sortedAll = computed(() => {
  const arr = [...filteredAll.value]
  if (!sortProp.value) return arr
  const dir = sortOrder.value === 'ascending' ? 1 : -1
  return arr.sort((a, b) => compareBy(a, b, sortProp.value) * dir)
})
const pagedAll = computed(() => sortedAll.value.slice((page.value - 1) * size.value, page.value * size.value))
function compareBy(a, b, prop) {
  const va = a[prop]
  const vb = b[prop]
  if (va == null && vb == null) return 0
  if (va == null) return -1
  if (vb == null) return 1
  if (typeof va === 'number' && typeof vb === 'number') return va - vb
  return String(va).localeCompare(String(vb)) // submittedAt 为 ISO 串、status 为码，字典序即可
}
function onSortChange({ prop, order }) {
  sortProp.value = order ? prop : 'submittedAt'
  sortOrder.value = order || 'descending'
  page.value = 1
}
// 筛选变化回到第一页，避免停留在越界的空页
watch([roleFilter, bizFilter, statusFilter, keyword], () => { page.value = 1 })

// change 功能码关闭时：回退到「全部」tab，并清理残留为「变更审批」的类型筛选
watch(showChange, (v) => {
  if (v) return
  if (activeTab.value === 'change') activeTab.value = 'all'
  if (bizFilter.value === 'change') bizFilter.value = ''
}, { immediate: true })

const lookupId = ref('')
const acting = ref(false)
const comment = ref('')
const members = ref([])

// 详情抽屉
const detailOpen = ref(false)
const detailRow = ref(null)   // 列表行（含角色标记，用于动作可见性）
const current = ref(null)     // InstanceVO（getInstance 详情）
const project = ref(null)
const stepsRef = ref()
const transferVisible = ref(false)
const transferring = ref(false)
const transferTo = ref('')
const transferComment = ref('')
const withdrawing = ref(false)

const detailTitle = computed(() => detailRow.value?.title || (current.value ? bizTypeLabel(current.value.bizType) : '审批详情'))
// 待我处理：实例 pending 且当前行标记待我审批
const canAct = computed(() => current.value?.status === 'pending' && detailRow.value?.mineToAct)
// 我发起且仍在审批中：可撤回
const canWithdraw = computed(() => current.value?.status === 'pending' && detailRow.value?.iInitiated)

// 审批实例状态码 → StatusTag 中文（pending/approved/rejected/withdrawn）
const STATUS_ZH = { pending: '审批中', approved: '已结案', rejected: '失败', withdrawn: '已撤回' }
const statusZh = (s) => STATUS_ZH[s] || s
const fmtDate = (v) => (v ? String(v).slice(0, 10) : '—')
const fmtMoney = (v) => (v == null ? '—' : Number(v).toLocaleString('zh-CN'))
const categoryLabel = (c) => PROJECT_CATEGORIES.find((p) => p.value === c)?.label || c || '—'

async function loadAll() {
  allLoading.value = true
  try {
    all.value = await approvalApi.mineAll() || []
  } finally {
    allLoading.value = false
  }
}

const route = useRoute()
// 顶部导航(WorkspaceShell)按 ?tab 切换内容；驱动 activeTab 跟随路由
watch(() => route.query.tab, (t) => {
  if (t === 'change') activeTab.value = showChange.value ? 'change' : 'all'
  else if (t === 'initiated') { activeTab.value = 'all'; roleFilter.value = 'initiated' } // 兼容旧深链
  else activeTab.value = 'all'
})

onMounted(async () => {
  // 旧 ?tab 深链兼容：change → 变更台账；initiated → 全部并预置「我发起的」筛选
  if (route.query.tab === 'change' && showChange.value) activeTab.value = 'change'
  else if (route.query.tab === 'initiated') roleFilter.value = 'initiated'
  try {
    bizTypes.value = await approvalApi.bizTypes() || []
  } catch {
    bizTypes.value = []
  }
  try {
    members.value = await fetchMembers()
  } catch {
    members.value = []
  }
  loadAll()
  // 支持从工作台/通知深链直接打开某审批实例
  if (route.query.open) openInstance(route.query.open)
})

// 打开详情：列表行 → 拉取实例 + 项目情况
async function openDetail(row) {
  detailRow.value = row
  detailOpen.value = true
  comment.value = ''
  await openInstance(row.instanceId)
}
// 仅有实例 ID（深链/查询打开）时：拉取后从列表回填行标记（无则置空，按只读详情展示）
async function openInstance(id) {
  current.value = null
  project.value = null
  detailOpen.value = true
  if (!detailRow.value || String(detailRow.value.instanceId) !== String(id)) {
    detailRow.value = all.value.find((r) => String(r.instanceId) === String(id)) || null
  }
  current.value = await approvalApi.getInstance(id)
  if (current.value?.bizType === 'project_init' && current.value.bizId != null) {
    try {
      project.value = await projectApi.get(current.value.bizId)
    } catch {
      project.value = null
    }
  }
}
function open() {
  if (lookupId.value) openInstance(lookupId.value)
}

// 刷新：重拉列表后回填当前行标记 + 重拉实例与进度
async function refreshCurrent() {
  await loadAll()
  if (current.value) {
    detailRow.value = all.value.find((r) => String(r.instanceId) === String(current.value.id)) || null
    current.value = await approvalApi.getInstance(current.value.id)
    stepsRef.value?.reload()
  }
}

async function act(action) {
  acting.value = true
  try {
    await approvalApi.act(current.value.id, { action, comment: comment.value })
    ElMessage.success(action === 'approve' ? '已通过' : '已驳回')
    comment.value = ''
    await refreshCurrent()
  } finally {
    acting.value = false
  }
}
async function doTransfer() {
  transferring.value = true
  try {
    await approvalApi.transfer(current.value.id, {
      toUserId: transferTo.value, comment: transferComment.value || null,
    })
    ElMessage.success('已转交')
    transferVisible.value = false
    transferTo.value = ''
    transferComment.value = ''
    await refreshCurrent()
  } finally {
    transferring.value = false
  }
}
async function doWithdraw() {
  withdrawing.value = true
  try {
    await approvalApi.withdraw(current.value.id, { reason: null })
    ElMessage.success('已撤回')
    await refreshCurrent()
  } finally {
    withdrawing.value = false
  }
}
</script>

<style scoped>
/* 顶部导航(WorkspaceShell)已提供 tab 切换，隐藏 el-tabs 自带头避免双层 */
.apv__tabs--headless :deep(.el-tabs__header) {
  display: none;
}
.apv__lookup {
  display: flex;
  gap: var(--mido-space-2);
}
.apv__input {
  width: var(--mido-nav-width);
}
/* 操作条：左角色切换、右筛选搜索（对齐项目模块 §7-A） */
.apv__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
}
.apv__bar-right {
  display: flex;
  gap: var(--mido-space-2);
}
.apv__quick {
  width: calc(var(--mido-nav-width) * 0.7);
}
.apv__search {
  width: var(--mido-nav-width);
}
.apv__obj {
  color: var(--el-text-color-primary);
  font-weight: var(--mido-font-weight-bold);
}
.apv__roles {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-1);
}
/* 卡片视图：自适应网格，与项目模块卡片一致的层次（顶部状态 + 标题 + 角色 + 页脚） */
.apv__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--mido-space-3);
}
.apv__card {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.apv__card:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}
.apv__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.apv__card-title {
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}
.apv__card-roles {
  margin-top: auto;
}
.apv__card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.apv__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
.apv__head {
  margin-bottom: var(--mido-space-4);
}
.apv__proj {
  margin-bottom: var(--mido-space-4);
}
.apv__drawer-steps {
  margin-top: var(--mido-space-4);
}
.apv__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--mido-space-2);
}
.apv__withdraw {
  margin-top: var(--mido-space-4);
}
.apv__warn {
  margin-top: var(--mido-space-4);
}
</style>
