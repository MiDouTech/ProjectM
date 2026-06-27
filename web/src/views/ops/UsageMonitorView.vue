<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">用量监控</h2>
      <div class="bar__right">
        <el-switch v-model="onlyExceeded" active-text="仅看超限" @change="reload" />
        <el-button :loading="snapshotting" @click="doSnapshot">立即快照</el-button>
      </div>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-skeleton v-else-if="loading && !rows.length" :rows="6" animated :throttle="300" />
    <template v-else>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="租户" min-width="180">
        <template #default="{ row }">
          <div>{{ row.tenantName }}</div>
          <div class="mut">{{ row.tenantCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" :label="tenantLabel(row.status)" /></template>
      </el-table-column>
      <el-table-column v-for="res in QUOTA_RESOURCE" :key="res.value" :label="res.label" min-width="140">
        <template #default="{ row }">
          <div class="usage-cell">
            <span class="usage-cell__num mido-mono" :class="{ over: cell(row, res.value).exceeded }">
              {{ cell(row, res.value).used }} / {{ fmtLimit(cell(row, res.value).limit) }}
            </span>
            <el-progress
              v-if="cell(row, res.value).limit >= 0"
              :percentage="pct(cell(row, res.value))"
              :status="cell(row, res.value).exceeded ? 'exception' : undefined"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="超限" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.anyExceeded" type="danger" effect="plain" size="small">超限</el-tag>
          <span v-else class="mut">正常</span>
        </template>
      </el-table-column>
      <template #empty><el-empty :description="onlyExceeded ? '无超限租户' : '暂无用量数据'" /></template>
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
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import { usageApi, QUOTA_RESOURCE, TENANT_STATUS } from '@/api/ops'

const loading = ref(false)
const loadError = ref(false)
const snapshotting = ref(false)
const onlyExceeded = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20 })

const EMPTY_CELL = { used: 0, limit: -1, exceeded: false }
function cell(row, resource) {
  return (row.usage || []).find((u) => u.resource === resource) || EMPTY_CELL
}
function fmtLimit(limit) {
  return limit < 0 ? '不限' : limit
}
function pct(c) {
  if (!c || c.limit <= 0) return 0
  return Math.min(100, Math.round((c.used / c.limit) * 100))
}
function tenantLabel(status) {
  return TENANT_STATUS.find((s) => s.value === status)?.label || ''
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const res = await usageApi.monitorQuery({
      page: query.page,
      size: query.size,
      onlyExceeded: onlyExceeded.value,
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

async function doSnapshot() {
  snapshotting.value = true
  try {
    const n = await usageApi.snapshot()
    ElMessage.success(`已刷新 ${n} 个租户用量`)
    load()
  } finally {
    snapshotting.value = false
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
  align-items: center;
  gap: var(--mido-space-3);
}
.mut {
  color: var(--el-text-color-secondary);
  font-size: var(--mido-font-size-caption);
}
.usage-cell {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.usage-cell__num {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-regular);
}
.over {
  color: var(--el-color-danger);
  font-weight: var(--mido-font-weight-bold);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
</style>
