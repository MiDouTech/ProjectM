<template>
  <div :class="{ 'mido-page': !embedded }">
    <div class="cc__bar">
      <h1 v-if="!embedded" class="mido-h1">变更中心</h1>
      <div class="cc__filters">
        <el-select v-model="status" clearable placeholder="全部状态" class="cc__status" @change="load">
          <el-option v-for="s in CHANGE_STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table :data="rows" class="is-clickable" @row-click="openDetail">
        <el-table-column label="变更摘要" min-width="220" prop="title" show-overflow-tooltip />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
        </el-table-column>
        <el-table-column label="对象" width="130">
          <template #default="{ row }">{{ bizLabel(row.bizType) }}#{{ row.bizId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="statusLabel(row.status)" /></template>
        </el-table-column>
        <el-table-column label="发起人" width="110">
          <template #default="{ row }">{{ userName(row.createBy) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无变更记录" /></template>
      </el-table>
    </el-card>

    <!-- 详情右抽屉：before→after diff + 事由 + 审批进度 -->
    <el-drawer v-model="detailOpen" :title="current.title" size="var(--mido-drawer-width)">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">{{ changeTypeLabel(current.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{ bizLabel(current.bizType) }}#{{ current.bizId }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="statusLabel(current.status)" /></el-descriptions-item>
        <el-descriptions-item label="事由">{{ current.reason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="影响分析">{{ current.impact || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.appliedAt" label="生效时间">{{ fmt(current.appliedAt) }}</el-descriptions-item>
      </el-descriptions>

      <div class="cc__diff">
        <span class="mido-h2">变更内容（前 → 后）</span>
        <el-table :data="diff" size="small">
          <el-table-column label="字段" width="120" prop="label" />
          <el-table-column label="变更前"><template #default="{ row }"><span class="mido-mono">{{ row.before }}</span></template></el-table-column>
          <el-table-column label="变更后"><template #default="{ row }"><b class="mido-mono">{{ row.after }}</b></template></el-table-column>
          <template #empty><el-empty description="无字段变更" :image-size="50" /></template>
        </el-table>
      </div>

      <div v-if="current.approvalInstanceId" class="cc__approval">
        <span class="mido-h2">审批进度</span>
        <ApprovalSteps :instance-id="current.approvalInstanceId" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ApprovalSteps from '@/components/ApprovalSteps.vue'
import { changeApi, CHANGE_STATUS, CHANGE_TYPES } from '@/api/change'
import { fetchMembers } from '@/api/org'
import { userName as nameOf, formatDateTime } from '@/utils/display'

// embedded：作为「审批中心」变更台账 Tab 嵌入时，隐藏自身标题与页面外边距
defineProps({ embedded: { type: Boolean, default: false } })

const loading = ref(false)
const rows = ref([])
const users = ref([])
const status = ref('')
const detailOpen = ref(false)
const current = ref({})

// 变更字段中文标签（与目标基线字段对应）
const FIELD_LABEL = {
  title: '标题', ownerId: '负责人', period: '周期',
  metricUnit: '单位', metricStart: '指标起点', metricTarget: '指标目标',
}

const userName = (id) => nameOf(users.value, id)
const changeTypeLabel = (t) => CHANGE_TYPES.find((x) => x.value === t)?.label || t
const statusLabel = (s) => CHANGE_STATUS.find((x) => x.value === s)?.label || s
const bizLabel = (b) => (b === 'goal' ? '目标' : b)
const fmt = (v) => formatDateTime(v) || '—'

// before→after：以 after_payload 的改动字段为准，对照 before_snapshot
const diff = computed(() => {
  const before = parse(current.value.beforeSnapshot)
  const after = parse(current.value.afterPayload)
  return Object.keys(after).map((k) => ({
    label: FIELD_LABEL[k] || k,
    before: fmtVal(k, before[k]),
    after: fmtVal(k, after[k]),
  }))
})

function parse(json) {
  if (!json) return {}
  try { return JSON.parse(json) } catch { return {} }
}
function fmtVal(key, v) {
  if (v == null || v === '') return '—'
  if (key === 'ownerId') return userName(v)
  return v
}

async function load() {
  loading.value = true
  try {
    rows.value = await changeApi.list({ status: status.value || undefined })
  } finally {
    loading.value = false
  }
}
function openDetail(row) {
  current.value = row
  detailOpen.value = true
}

onMounted(async () => {
  users.value = await fetchMembers()
  load()
})
</script>

<style scoped>
.cc__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.cc__filters {
  display: flex;
  gap: var(--mido-space-2);
}
.cc__status {
  width: calc(var(--mido-admin-nav-width) * 0.8);
}
.cc__diff {
  margin-top: var(--mido-space-5);
}
.cc__approval {
  margin-top: var(--mido-space-5);
}
</style>
