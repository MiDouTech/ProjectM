<template>
  <div class="mido-page" v-loading="loading">
    <WorkspaceShell module="report" />

    <!-- KPI 卡 -->
    <div class="rpt__cards" v-if="ov">
      <el-card shadow="never" class="rpt__card">
        <div class="rpt__metric">
          <span class="rpt__num mido-mono">{{ fmt(ov.completionRate) }}%</span>
          <span class="mido-text-secondary">任务完成率</span>
        </div>
      </el-card>
      <el-card shadow="never" class="rpt__card">
        <div class="rpt__metric">
          <span class="rpt__num mido-mono" :class="{ 'rpt__num--warn': Number(ov.overdueRate) > 10 }">
            {{ fmt(ov.overdueRate) }}%
          </span>
          <span class="mido-text-secondary">任务逾期率</span>
        </div>
      </el-card>
      <el-card shadow="never" class="rpt__card">
        <div class="rpt__metric">
          <span class="rpt__num mido-mono">{{ ov.taskTotal }}</span>
          <span class="mido-text-secondary">任务总数（完成 {{ ov.completed }} / 逾期 {{ ov.overdue }}）</span>
        </div>
      </el-card>
      <el-card shadow="never" class="rpt__card">
        <div class="rpt__metric">
          <span class="rpt__num mido-mono">{{ projectTotal }}</span>
          <span class="mido-text-secondary">项目总数</span>
        </div>
      </el-card>
    </div>

    <!-- S/I/O 分布 -->
    <el-card shadow="never" class="rpt__block">
      <div class="mido-h2">项目类型分布（S/I/O）</div>
      <G2Chart v-if="distData.length" :option="distOption" :height="260" />
      <el-empty v-else description="暂无项目" :image-size="60" />
    </el-card>

    <!-- 项目燃尽 + 健康 -->
    <el-card shadow="never" class="rpt__block">
      <div class="rpt__row">
        <div class="mido-h2">项目燃尽图 / 健康度</div>
        <el-select v-model="projectId" filterable placeholder="选择项目" class="rpt__sel" @change="loadProject">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </div>
      <template v-if="projectId">
        <div class="rpt__health" v-if="health">
          <StatusTag :status="health.healthLabel" />
          <span class="mido-text-secondary">完成 <b class="mido-mono">{{ fmt(health.completionRate) }}%</b>
            · 逾期 <b class="mido-mono">{{ fmt(health.overdueRate) }}%</b>
            · 预算使用 <b class="mido-mono">{{ health.budgetUsage == null ? '—' : fmt(health.budgetUsage) + '%' }}</b></span>
        </div>
        <G2Chart v-if="burndownData.length" :option="burndownOption" :height="280" />
        <el-empty v-else description="该项目无带截止日的任务，无法绘制燃尽图" :image-size="60" />
      </template>
      <el-empty v-else description="选择项目查看燃尽图与健康度" :image-size="60" />
    </el-card>

    <!-- PMO 总体评价 -->
    <el-card shadow="never" class="rpt__block" v-if="pmo">
      <div class="rpt__row">
        <div class="mido-h2">PMO 总体评价</div>
        <div class="rpt__pmo-ctl">
          <!-- 任意周期：选区间则动态按区间统计，清空回到财年口径 -->
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" unlink-panels
            range-separator="~" start-placeholder="起" end-placeholder="止" size="default"
            class="rpt__range" @change="onRangeChange" />
          <el-select v-if="!range" v-model="year" class="rpt__sel" @change="loadPmo">
            <el-option v-for="y in years" :key="y" :label="`${y} 财年`" :value="y" />
          </el-select>
        </div>
      </div>
      <div class="rpt__pmo">
        <div class="rpt__metric">
          <span class="rpt__num mido-mono" :class="pmo.aboveBaseline ? 'rpt__num--ok' : 'rpt__num--warn'">
            {{ fmt(pmo.pmoNpss) }}
          </span>
          <span class="mido-text-secondary">PMO NPSS（成功% − 失败%）</span>
          <StatusTag :status="pmo.aboveBaseline ? '达标' : '未达标'" />
        </div>
        <span class="mido-text-secondary">基线 {{ fmt(pmo.baseline) }} · 已验收 {{ pmo.total }}
          （成功 {{ pmo.success }} / 混合 {{ pmo.mixed }} / 失败 {{ pmo.failure }}）</span>
      </div>
    </el-card>

    <!-- 人员负荷（对标 Worktile 人员报表）：数据范围内按负责人聚合在办/逾期 -->
    <el-card shadow="never" class="rpt__block">
      <div class="mido-h2">人员负荷（在办任务）</div>
      <el-table v-if="workload.length" :data="workload" stripe>
        <el-table-column label="负责人" min-width="160">
          <template #default="{ row }">{{ userName(row.assigneeId) }}</template>
        </el-table-column>
        <el-table-column label="在办任务" width="120" align="right">
          <template #default="{ row }"><span class="mido-mono">{{ row.inProgress }}</span></template>
        </el-table-column>
        <el-table-column label="其中逾期" width="120" align="right">
          <template #default="{ row }">
            <span class="mido-mono" :class="{ 'rpt__num--warn': row.overdue > 0 }">{{ row.overdue }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="数据范围内暂无在办任务" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import G2Chart from '@/components/G2Chart.vue'
import StatusTag from '@/components/StatusTag.vue'
import { reportApi } from '@/api/npss'
import { projectApi } from '@/api/project'
import { fetchMembers } from '@/api/org'

const CATEGORY_LABEL = { S: '战略级', I: '创新级', O: '运营级' }

const loading = ref(false)
const ov = ref(null)
const projects = ref([])
const projectId = ref(null)
const burndownData = ref([])
const health = ref(null)
const workload = ref([])

const users = ref([])
const userMap = computed(() => Object.fromEntries(users.value.map((u) => [u.id, u.name])))
const userName = (id) => userMap.value[id] || (id ? `用户#${id}` : '—')

const nowYear = new Date().getFullYear()
const years = Array.from({ length: 5 }, (_, i) => nowYear - i)
const year = ref(nowYear)
const range = ref(null) // [from, to] 任意周期；为空则用财年
const pmo = ref(null)

const fmt = (v) => (v == null ? '—' : Number(v))

const distData = computed(() =>
  (ov.value?.categoryDistribution || []).map((c) => ({
    type: CATEGORY_LABEL[c.category] || c.category || '未分类', count: c.count,
  })))
const projectTotal = computed(() => distData.value.reduce((n, d) => n + d.count, 0))

const distOption = computed(() => ({
  type: 'interval',
  data: distData.value,
  encode: { x: 'type', y: 'count', color: 'type' },
  axis: { y: { title: '项目数' }, x: { title: null } },
  legend: false,
}))
const burndownOption = computed(() => ({
  type: 'line',
  data: burndownData.value,
  encode: { x: 'date', y: 'remaining' },
  axis: { y: { title: '剩余任务' }, x: { title: '截止日' } },
  style: { lineWidth: 2 },
}))

async function load() {
  loading.value = true
  try {
    const [overview, page, wl, members] = await Promise.all([
      reportApi.overview(),
      projectApi.query({ page: 1, size: 200 }),
      reportApi.workload(),
      fetchMembers(),
    ])
    ov.value = overview
    projects.value = page.list || []
    workload.value = wl || []
    users.value = members || []
  } finally {
    loading.value = false
  }
}
async function loadProject() {
  if (!projectId.value) return
  const [bd, h] = await Promise.all([
    reportApi.burndown(projectId.value),
    reportApi.projectHealth(projectId.value),
  ])
  burndownData.value = bd.points || []
  health.value = h
}
async function loadPmo() {
  pmo.value = await reportApi.pmoNpss(year.value)
}
// to 为开区间，后端按 [from, to) 统计；选择的截止日 +1 天纳入当天
async function loadPmoRange() {
  const [from, to] = range.value
  const toExclusive = new Date(`${to}T00:00:00`)
  toExclusive.setDate(toExclusive.getDate() + 1)
  const toStr = toExclusive.toISOString().slice(0, 10)
  pmo.value = await reportApi.pmoNpssRange(from, toStr)
}
function onRangeChange(v) {
  if (v && v.length === 2) loadPmoRange()
  else loadPmo()
}

onMounted(() => {
  load()
  loadPmo()
})
</script>

<style scoped>
.rpt__cards {
  display: flex;
  gap: var(--mido-space-4);
  margin: var(--mido-space-4) 0;
}
.rpt__card {
  flex: 1;
}
.rpt__block {
  margin-bottom: var(--mido-space-4);
}
.rpt__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.rpt__sel {
  width: var(--mido-nav-width);
}
.rpt__metric {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  align-items: flex-start;
}
.rpt__num {
  font-size: var(--mido-font-size-h1);
  font-weight: var(--mido-font-weight-bold);
}
.rpt__num--ok { color: var(--el-color-success); }
.rpt__num--warn { color: var(--el-color-warning); }
.rpt__health {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
}
.rpt__pmo {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
}
.rpt__pmo-ctl {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.rpt__range {
  width: auto;
}
</style>
