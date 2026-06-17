<template>
  <div class="mido-page" v-loading="loading">
    <div class="rpt__bar">
      <h1 class="mido-h1">报表 · PMO 总体评价</h1>
      <el-select v-model="year" class="rpt__year" @change="load">
        <el-option v-for="y in years" :key="y" :label="`${y} 财年`" :value="y" />
      </el-select>
    </div>

    <template v-if="data">
      <!-- PMO NPSS 总分 vs 基线 36 -->
      <div class="rpt__cards">
        <el-card shadow="never" class="rpt__card">
          <div class="rpt__metric">
            <span class="rpt__num mido-mono" :class="data.aboveBaseline ? 'rpt__num--ok' : 'rpt__num--warn'">
              {{ fmt(data.pmoNpss) }}
            </span>
            <span class="mido-text-secondary">PMO NPSS（成功% − 失败%）</span>
            <StatusTag :status="data.aboveBaseline ? '达标' : '未达标'" />
          </div>
        </el-card>
        <el-card shadow="never" class="rpt__card">
          <div class="rpt__metric">
            <span class="rpt__num mido-mono">{{ fmt(data.baseline) }}</span>
            <span class="mido-text-secondary">全球基线（目标 &gt; 36）</span>
          </div>
        </el-card>
        <el-card shadow="never" class="rpt__card">
          <div class="rpt__metric">
            <span class="rpt__num mido-mono">{{ data.total }}</span>
            <span class="mido-text-secondary">已验收项目数</span>
          </div>
        </el-card>
      </div>

      <!-- 项目分布 -->
      <el-card shadow="never">
        <div class="mido-h2 rpt__dist-title">项目分布</div>
        <div v-for="d in dist" :key="d.key" class="rpt__dist">
          <StatusTag :status="d.label" />
          <el-progress :percentage="d.pct" :status="d.barStatus" class="rpt__dist-bar" />
          <span class="mido-mono rpt__dist-count">{{ d.count }}（{{ d.pct }}%）</span>
        </div>
        <el-empty v-if="!data.total" description="该财年暂无已验收项目" :image-size="60" />
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="暂无数据" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { reportApi } from '@/api/npss'

const nowYear = new Date().getFullYear()
const years = Array.from({ length: 5 }, (_, i) => nowYear - i)
const year = ref(nowYear)
const loading = ref(false)
const data = ref(null)

const fmt = (v) => (v == null ? '—' : Number(v))

// 分布：成功/混合/失败（占比 = 各自数量 / 总数）
const dist = computed(() => {
  const d = data.value
  if (!d || !d.total) return []
  const r = (n) => Math.round((n / d.total) * 100)
  return [
    { key: 'success', label: '成功', count: d.success, pct: r(d.success), barStatus: 'success' },
    { key: 'mixed', label: '混合', count: d.mixed, pct: r(d.mixed), barStatus: 'warning' },
    { key: 'failure', label: '失败', count: d.failure, pct: r(d.failure), barStatus: 'exception' },
  ]
})

async function load() {
  loading.value = true
  try {
    data.value = await reportApi.pmoNpss(year.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rpt__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.rpt__year {
  width: var(--mido-admin-nav-width);
}
.rpt__cards {
  display: flex;
  gap: var(--mido-space-4);
  margin-bottom: var(--mido-space-4);
}
.rpt__card {
  flex: 1;
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
.rpt__dist-title {
  margin-bottom: var(--mido-space-3);
}
.rpt__dist {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-2);
}
.rpt__dist-bar {
  flex: 1;
}
.rpt__dist-count {
  min-width: calc(var(--mido-admin-nav-width) * 0.6);
  text-align: right;
}
</style>
