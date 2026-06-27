<template>
  <div v-loading="loading" class="dash">
    <ErrorState v-if="loadError" @retry="load" />
    <template v-else>
    <!-- 顶部指标卡 -->
    <div class="dash__metrics">
      <el-card v-for="m in metrics" :key="m.key" shadow="never" class="metric mido-hoverable"
        @click="goTenants(m.status)">
        <div class="metric__label">{{ m.label }}</div>
        <div class="metric__value mido-mono">{{ m.value }}</div>
      </el-card>
    </div>

    <!-- 状态分布 -->
    <el-card shadow="never" class="dash__block">
      <template #header><span class="mido-h2">租户状态分布</span></template>
      <div class="dist">
        <div v-for="d in statusDist" :key="d.key" class="dist__row dist__row--click"
          @click="goTenants(d.key)">
          <div class="dist__head">
            <StatusTag :status="d.key" />
            <span class="dist__count">{{ d.value }}</span>
          </div>
          <div class="dist__bar">
            <div class="dist__bar-fill" :style="{ width: barWidth(d.value) }"></div>
          </div>
        </div>
        <el-empty v-if="!totalDist" description="暂无数据" />
      </div>
    </el-card>

    <!-- 近30天到期租户 -->
    <el-card shadow="never" class="dash__block">
      <template #header><span class="mido-h2">近 30 天到期租户</span></template>
      <el-table :data="expiringSoon" stripe>
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" :label="tenantLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="planName" label="套餐" width="140">
          <template #default="{ row }">{{ row.planName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="expireAt" label="到期时间" width="180">
          <template #default="{ row }">{{ row.expireAt || '不限期' }}</template>
        </el-table-column>
        <template #empty><el-empty description="近 30 天无到期租户" /></template>
      </el-table>
    </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '@/components/StatusTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import { dashboardApi, TENANT_STATUS } from '@/api/ops'

const router = useRouter()
const loading = ref(false)
const loadError = ref(false)
const overview = ref({})

const metrics = computed(() => [
  { key: 'total', label: '租户总数', value: Number(overview.value.totalTenants || 0), status: '' },
  { key: 'active', label: '正式租户', value: Number(overview.value.activeTenants || 0), status: 'active' },
  { key: 'trial', label: '试用租户', value: Number(overview.value.trialTenants || 0), status: 'trial' },
  { key: 'new', label: '本月新增', value: Number(overview.value.newThisMonth || 0), status: '' },
])

// 指标/状态下钻：跳租户管理并按状态预筛选
function goTenants(status) {
  router.push({ path: '/ops/tenants', query: status ? { status } : {} })
}

const statusDist = computed(() => {
  const dist = overview.value.statusDist || {}
  return TENANT_STATUS.map((s) => ({ key: s.value, value: Number(dist[s.value] || 0) }))
})
const totalDist = computed(() => statusDist.value.reduce((sum, d) => sum + d.value, 0))
const maxDist = computed(() => Math.max(1, ...statusDist.value.map((d) => d.value)))
function barWidth(v) {
  return `${Math.round((v / maxDist.value) * 100)}%`
}

const expiringSoon = computed(() => overview.value.expiringSoon || [])
function tenantLabel(status) {
  return TENANT_STATUS.find((s) => s.value === status)?.label || ''
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    overview.value = await dashboardApi.overview()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-5);
}

.dash__metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--mido-space-4);
}

.metric {
  text-align: center;
}
.metric__label {
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-secondary);
}
.metric__value {
  margin-top: var(--mido-space-2);
  font-size: var(--mido-font-size-h1);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-color-primary);
}

.dist {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
}
.dist__row {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.dist__row--click {
  cursor: pointer;
  border-radius: var(--mido-radius-sm);
  transition: background-color var(--mido-duration) var(--mido-ease);
}
.dist__row--click:hover {
  background-color: var(--el-fill-color-light);
}
.dist__head {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.dist__count {
  font-size: var(--mido-font-size-secondary);
  color: var(--el-text-color-regular);
}
.dist__bar {
  height: var(--mido-space-2);
  background-color: var(--el-fill-color-light);
  border-radius: var(--mido-radius-sm);
  overflow: hidden;
}
.dist__bar-fill {
  height: 100%;
  background-color: var(--el-color-primary);
  border-radius: var(--mido-radius-sm);
  transition: width var(--mido-duration) var(--mido-ease);
}

@media (max-width: 1024px) {
  .dash__metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
