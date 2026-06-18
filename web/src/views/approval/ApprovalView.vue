<template>
  <div class="mido-page">
    <div class="apv__bar">
      <h1 class="mido-h1">立项审批</h1>
      <div class="apv__lookup">
        <el-input v-model="lookupId" placeholder="输入审批实例 ID" class="apv__input"
          :prefix-icon="Search" @keyup.enter="open" />
        <el-button type="primary" :disabled="!lookupId" @click="open">打开</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 待我审批（当前登录人未处理且实例 pending 的待办） + 会话内最近打开 -->
      <el-col :span="8">
        <el-card shadow="never" v-loading="mineLoading">
          <h3 class="mido-h2">待我审批（{{ mine.length }}）</h3>
          <el-table :data="mine" @row-click="(r) => select(r.instanceId)">
            <el-table-column label="实例" width="90">
              <template #default="{ row }"><span class="mido-mono">#{{ row.instanceId }}</span></template>
            </el-table-column>
            <el-table-column label="业务" prop="bizType" />
            <el-table-column label="提交" width="110">
              <template #default="{ row }">{{ fmtDate(row.submittedAt) }}</template>
            </el-table-column>
            <template #empty><el-empty description="暂无待我审批" :image-size="60" /></template>
          </el-table>
        </el-card>

        <el-card shadow="never" class="apv__recent">
          <h3 class="mido-h2">最近打开</h3>
          <el-table :data="recent" @row-click="(r) => select(r.id)">
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
              <h3 class="mido-h2">审批实例 #{{ current.id }}</h3>
              <span class="mido-text-secondary">业务：{{ current.bizType }} / #{{ current.bizId }} · 申请人：{{ current.applicantId }}</span>
            </div>
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

    <!-- 转交：受让人暂以用户 ID 指定（统一选人组件待 Step 1） -->
    <el-dialog v-model="transferVisible" title="转交审批" width="420">
      <el-form label-width="92">
        <el-form-item label="受让人 ID" required>
          <el-input v-model="transferTo" placeholder="输入受让人用户 ID" />
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ApprovalSteps from '@/components/ApprovalSteps.vue'
import { approvalApi } from '@/api/project'

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

// 审批实例状态码 → StatusTag 中文（pending/approved/rejected）
const STATUS_ZH = { pending: '审批中', approved: '已结案', rejected: '失败' }
const statusZh = (s) => STATUS_ZH[s] || s
const fmtDate = (v) => (v ? String(v).slice(0, 10) : '—')

async function loadMine() {
  mineLoading.value = true
  try {
    mine.value = await approvalApi.mine() || []
  } finally {
    mineLoading.value = false
  }
}
onMounted(loadMine)

async function loadInstance(id) {
  loading.value = true
  try {
    current.value = await approvalApi.getInstance(id)
    // 维护会话内最近列表
    recent.value = [current.value, ...recent.value.filter((r) => r.id !== current.value.id)].slice(0, 10)
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
.apv__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--mido-space-2);
}
.apv__warn {
  margin-top: var(--mido-space-4);
}
</style>
