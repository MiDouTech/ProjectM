<template>
  <div class="mido-page" v-loading="loading">
    <h1 class="mido-h1">报表 · 度量仪表盘</h1>

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
        <el-select v-model="year" class="rpt__sel" @change="loadPmo">
          <el-option v-for="y in years" :key="y" :label="`${y} 财年`" :value="y" />
        </el-select>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import G2Chart from '@/components/G2Chart.vue'
import StatusTag from '@/components/StatusTag.vue'
import { reportApi } from '@/api/npss'
import { projectApi } from '@/api/project'

const CATEGORY_LABEL = { S: '战略级', I: '创新级', O: '运营级' }

const loading = ref(false)
const ov = ref(null)
const projects = ref([])
const projectId = ref(null)
const burndownData = ref([])
const health = ref(null)

const nowYear = new Date().getFullYear()
const years = Array.from({ length: 5 }, (_, i) => nowYear - i)
const year = ref(nowYear)
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
    const [overview, page] = await Promise.all([
      reportApi.overview(),
      projectApi.query({ page: 1, size: 200 }),
    ])
    ov.value = overview
    projects.value = page.list || []
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
</style>
