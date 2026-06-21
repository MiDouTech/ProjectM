<template>
  <div class="mido-page">
    <div class="apv__bar">
      <h1 class="mido-h1">审批中心</h1>
      <div v-show="activeTab === 'mine'" class="apv__lookup">
        <el-input v-model="lookupId" placeholder="输入审批实例 ID" class="apv__input"
          :prefix-icon="Search" @keyup.enter="open" />
        <el-button type="primary" :disabled="!lookupId" @click="open">打开</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="apv__tabs">
      <el-tab-pane label="待我审批" name="mine">
    <el-row :gutter="16">
      <!-- 待我审批（当前登录人未处理且实例 pending 的待办） + 会话内最近打开 -->
      <el-col :span="8">
        <el-card shadow="never" v-loading="mineLoading">
          <div class="apv__row">
            <h3 class="mido-h2">待我审批（{{ filteredMine.length }}）</h3>
            <el-select v-model="mineBizFilter" clearable placeholder="全部类型" class="apv__filter">
              <el-option v-for="b in bizFilterOptions" :key="b.value" :label="b.label" :value="b.value" />
            </el-select>
          </div>
          <el-table :data="filteredMine" class="is-clickable" @row-click="(r) => select(r.instanceId)">
            <el-table-column label="审批对象" min-width="140">
              <template #default="{ row }">
                <div class="apv__obj">{{ row.title || bizTypeLabel(row.bizType) }}</div>
                <div class="mido-text-secondary">{{ bizTypeLabel(row.bizType) }} · #{{ row.instanceId }}</div>
              </template>
            </el-table-column>
            <el-table-column label="提交" width="100">
              <template #default="{ row }">{{ fmtDate(row.submittedAt) }}</template>
            </el-table-column>
            <template #empty><el-empty description="暂无待我审批" :image-size="60" /></template>
          </el-table>
        </el-card>

        <el-card shadow="never" class="apv__recent">
          <h3 class="mido-h2">最近打开</h3>
          <el-table :data="recent" class="is-clickable" @row-click="(r) => select(r.id)">
            <el-table-column label="实例" prop="id" width="90">
              <template #default="{ row }"><span class="mido-mono">#{{ row.id }}</span></template>
            </el-table-column>
            <el-table-column label="业务" prop="bizType" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="statusZh(row.status)" /></template>
            </el-table-column>
            <template #empty><el-empty description="尚未打开任何审批" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-col>

      <!-- 进度 + 审批动作 -->
      <el-col :span="16">
        <el-card shadow="never" v-loading="loading">
          <template v-if="current">
            <div class="apv__head">
              <h3 class="mido-h2">{{ project?.name || bizTypeLabel(current.bizType) }}</h3>
              <span class="mido-text-secondary">{{ bizTypeLabel(current.bizType) }} · 实例 #{{ current.id }} · 申请人：{{ userName(members, current.applicantId) }}</span>
            </div>

            <!-- 项目情况：让审批人看到立项的关键信息再决策（立项审批专属） -->
            <el-descriptions v-if="project" class="apv__proj" :column="3" border size="small">
              <el-descriptions-item label="项目编号"><span class="mido-mono">{{ project.code || '—' }}</span></el-descriptions-item>
              <el-descriptions-item label="类型">{{ categoryLabel(project.category) }}<template v-if="project.subCategory"> / {{ project.subCategory }}</template></el-descriptions-item>
              <el-descriptions-item label="状态"><StatusTag :status="project.status" /></el-descriptions-item>
              <el-descriptions-item label="负责人">{{ userName(members, project.leaderId) }}</el-descriptions-item>
              <el-descriptions-item label="预算(元)"><span class="mido-mono">{{ fmtMoney(project.budget) }}</span></el-descriptions-item>
              <el-descriptions-item label="周期">{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="项目目标" :span="3">{{ current.formData?.objective || '—' }}</el-descriptions-item>
              <el-descriptions-item v-if="current.formData?.valueHypothesis" label="价值假设" :span="3">{{ current.formData.valueHypothesis }}</el-descriptions-item>
            </el-descriptions>

            <ApprovalSteps ref="stepsRef" :instance-id="current.id" />

            <template v-if="current.status === 'pending'">
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
              <el-alert class="apv__warn" type="warning" :closable="false" show-icon
                title="严肃提示" description="未通过审批，项目不得进入执行态。驳回将退回申请人修改。" />
            </template>
          </template>
          <el-empty v-else description="输入审批实例 ID 打开，或从左侧最近打开中选择" />
        </el-card>
      </el-col>
    </el-row>
      </el-tab-pane>
      <!-- 我发起的：跨 bizType 列出我提交的审批实例（任意状态），可查进度 / 撤回待审 -->
      <el-tab-pane label="我发起的" name="initiated" lazy>
        <el-card shadow="never" v-loading="initiatedLoading">
          <div class="apv__row">
            <h3 class="mido-h2">我发起的审批（{{ filteredInitiated.length }}）</h3>
            <el-select v-model="initiatedBizFilter" clearable placeholder="全部类型" class="apv__filter">
              <el-option v-for="b in bizFilterOptions" :key="b.value" :label="b.label" :value="b.value" />
            </el-select>
          </div>
          <el-table :data="filteredInitiated" class="is-clickable" @row-click="openInitiated">
            <el-table-column label="审批对象" min-width="160">
              <template #default="{ row }">
                <div class="apv__obj">{{ row.title || bizTypeLabel(row.bizType) }}</div>
                <div class="mido-text-secondary">{{ bizTypeLabel(row.bizType) }} · #{{ row.instanceId }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="statusZh(row.status)" /></template>
            </el-table-column>
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ fmtDate(row.submittedAt) }}</template>
            </el-table-column>
            <template #empty><el-empty description="我尚未发起任何审批" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>
      <!-- 变更台账：复用变更中心组件（embedded 隐藏自身标题）；受 change 功能码门控 -->
      <el-tab-pane v-if="showChange" label="变更台账" name="change" lazy>
        <ChangeCenter embedded />
      </el-tab-pane>
    </el-tabs>

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

    <!-- 我发起的：进度抽屉，pending 可由发起人本人撤回 -->
    <el-drawer v-model="initiatedDrawer" :title="initiatedCurrent?.title || '审批进度'" size="var(--mido-drawer-width)">
      <template v-if="initiatedCurrent">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="类型">{{ bizTypeLabel(initiatedCurrent.bizType) }}</el-descriptions-item>
          <el-descriptions-item label="对象"><span class="mido-mono">#{{ initiatedCurrent.bizId }}</span></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="statusZh(initiatedCurrent.status)" /></el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDate(initiatedCurrent.submittedAt) }}</el-descriptions-item>
        </el-descriptions>
        <ApprovalSteps :instance-id="initiatedCurrent.instanceId" class="apv__drawer-steps" />
        <div v-if="initiatedCurrent.status === 'pending'" class="apv__actions">
          <el-button type="danger" plain :loading="withdrawing" @click="doWithdraw">撤回申请</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ApprovalSteps from '@/components/ApprovalSteps.vue'
import UserSelect from '@/components/UserSelect.vue'
import ChangeCenter from '@/views/ChangeCenter.vue'
import { approvalApi, projectApi, PROJECT_CATEGORIES } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { userName } from '@/utils/display'
import { useUserStore } from '@/store/user'

// 审批中心 Tab：待我审批 / 我发起的 / 变更台账（变更受 change 功能码门控，fail-open）
const userStore = useUserStore()
const showChange = computed(() => userStore.hasFeature('change'))
const activeTab = ref('mine')
// bizType 字典：单一信息源，挂载时从后端 /approvals/biz-types 拉取（不再前端硬编码）
const bizTypes = ref([])
// 筛选项随 showChange 收敛：无 change 功能时剔除「变更审批」死选项
const bizFilterOptions = computed(() => (showChange.value
  ? bizTypes.value
  : bizTypes.value.filter((b) => b.value !== 'change')))

// 待我审批按 bizType 客户端筛选（数据来自 mine，不增后端请求）
const mineBizFilter = ref('')
const filteredMine = computed(() => (mineBizFilter.value
  ? mine.value.filter((r) => r.bizType === mineBizFilter.value)
  : mine.value))

// 我发起的：跨 bizType 列表 + 进度/撤回抽屉
const initiated = ref([])
const initiatedLoading = ref(false)
const initiatedBizFilter = ref('')
const filteredInitiated = computed(() => (initiatedBizFilter.value
  ? initiated.value.filter((r) => r.bizType === initiatedBizFilter.value)
  : initiated.value))
const initiatedDrawer = ref(false)
const initiatedCurrent = ref(null)
const withdrawing = ref(false)

async function loadInitiated() {
  initiatedLoading.value = true
  try {
    initiated.value = await approvalApi.mineInitiated() || []
  } catch {
    // 错误已由 request 拦截器统一 toast；此处吞掉避免 watch 中未捕获 rejection
  } finally {
    initiatedLoading.value = false
  }
}
function openInitiated(row) {
  initiatedCurrent.value = row
  initiatedDrawer.value = true
}
async function doWithdraw() {
  withdrawing.value = true
  try {
    await approvalApi.withdraw(initiatedCurrent.value.instanceId, { reason: null })
    ElMessage.success('已撤回')
    initiatedDrawer.value = false
    await loadInitiated()
    loadMine()
  } finally {
    withdrawing.value = false
  }
}
// 切到「我发起的」时拉取（保持状态新鲜；列表轻量，重复请求成本低）
watch(activeTab, (t) => {
  if (t === 'initiated') loadInitiated()
})
// change 功能码关闭时（含异步翻转/挂载即关闭）：回退变更台账 tab，并清理残留为
// 「变更审批」的筛选值，避免内容区空白或筛选恒空。immediate 兼顾挂载时已为 false 的情形。
watch(showChange, (v) => {
  if (v) return
  if (activeTab.value === 'change') activeTab.value = 'mine'
  if (mineBizFilter.value === 'change') mineBizFilter.value = ''
  if (initiatedBizFilter.value === 'change') initiatedBizFilter.value = ''
}, { immediate: true })

const lookupId = ref('')
const loading = ref(false)
const acting = ref(false)
const comment = ref('')
const current = ref(null)
const recent = ref([])
const stepsRef = ref()
const mine = ref([])
const mineLoading = ref(false)
const transferVisible = ref(false)
const transferring = ref(false)
const transferTo = ref('')
const transferComment = ref('')
const project = ref(null)
const members = ref([])

// 审批实例状态码 → StatusTag 中文（pending/approved/rejected）
const STATUS_ZH = { pending: '审批中', approved: '已结案', rejected: '失败', withdrawn: '已撤回' }
const statusZh = (s) => STATUS_ZH[s] || s
const fmtDate = (v) => (v ? String(v).slice(0, 10) : '—')
const fmtMoney = (v) => (v == null ? '—' : Number(v).toLocaleString('zh-CN'))
// bizType / 项目类型 码 → 中文标签
const bizTypeLabel = (t) => bizTypes.value.find((b) => b.value === t)?.label || t
const categoryLabel = (c) => PROJECT_CATEGORIES.find((p) => p.value === c)?.label || c || '—'

async function loadMine() {
  mineLoading.value = true
  try {
    mine.value = await approvalApi.mine() || []
  } finally {
    mineLoading.value = false
  }
}
const route = useRoute()
onMounted(async () => {
  // 旧 /change 深链重定向至此，按 tab 参数定位变更台账（无 change 功能则不切，留在待我审批）
  if (route.query.tab === 'change' && showChange.value) activeTab.value = 'change'
  try {
    bizTypes.value = await approvalApi.bizTypes() || []
  } catch {
    bizTypes.value = []
  }
  loadMine()
  try {
    members.value = await fetchMembers()
  } catch {
    members.value = []
  }
  // 支持从工作台/通知深链直接打开某审批实例
  if (route.query.open) loadInstance(route.query.open)
})

async function loadInstance(id) {
  loading.value = true
  try {
    current.value = await approvalApi.getInstance(id)
    // 维护会话内最近列表
    recent.value = [current.value, ...recent.value.filter((r) => r.id !== current.value.id)].slice(0, 10)
    // 立项审批：拉取项目情况供审批人决策（其他 bizType 暂不展示业务卡）
    project.value = null
    if (current.value?.bizType === 'project_init' && current.value.bizId != null) {
      try {
        project.value = await projectApi.get(current.value.bizId)
      } catch {
        project.value = null
      }
    }
  } finally {
    loading.value = false
  }
}
function open() {
  if (lookupId.value) loadInstance(lookupId.value)
}
function select(id) {
  loadInstance(id)
}
async function act(action) {
  acting.value = true
  try {
    await approvalApi.act(current.value.id, { action, comment: comment.value })
    ElMessage.success(action === 'approve' ? '已通过' : '已驳回')
    comment.value = ''
    await loadInstance(current.value.id)
    stepsRef.value?.reload()
    loadMine()
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
    await loadInstance(current.value.id)
    stepsRef.value?.reload()
    loadMine()
  } finally {
    transferring.value = false
  }
}
</script>

<style scoped>
.apv__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.apv__recent {
  margin-top: var(--mido-space-4);
}
.apv__lookup {
  display: flex;
  gap: var(--mido-space-2);
}
.apv__input {
  width: var(--mido-nav-width);
}
.apv__note {
  margin-bottom: var(--mido-space-4);
}
.apv__head {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
  margin-bottom: var(--mido-space-4);
}
.apv__obj {
  color: var(--el-text-color-primary);
  font-weight: var(--mido-font-weight-bold);
}
.apv__proj {
  margin-bottom: var(--mido-space-4);
}
.apv__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--mido-space-2);
}
.apv__warn {
  margin-top: var(--mido-space-4);
}
.apv__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.apv__filter {
  width: calc(var(--mido-nav-width) * 0.7);
}
.apv__drawer-steps {
  margin-top: var(--mido-space-4);
}
</style>
