<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">操作日志</h2>
      <div class="bar__right">
        <el-select v-model="query.userId" placeholder="操作人" clearable filterable
          class="bar__filter" @change="reload">
          <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
        </el-select>
        <el-select v-model="query.module" placeholder="模块" clearable class="bar__filter--sm" @change="reload">
          <el-option v-for="o in AUDIT_MODULES" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.action" placeholder="动作" clearable class="bar__filter--sm" @change="reload">
          <el-option v-for="o in AUDIT_ACTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker v-model="range" type="datetimerange" unlink-panels
          start-placeholder="起始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss"
          class="bar__range" @change="reload" />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作人" width="120">
        <template #default="{ row }">{{ row.userName || '—' }}</template>
      </el-table-column>
      <el-table-column label="模块" width="110">
        <template #default="{ row }"><StatusTag :status="row.module" :label="label(AUDIT_MODULES, row.module)" /></template>
      </el-table-column>
      <el-table-column label="动作" width="130">
        <template #default="{ row }">{{ label(AUDIT_ACTIONS, row.action) }}</template>
      </el-table-column>
      <el-table-column label="对象" min-width="160">
        <template #default="{ row }">
          {{ label(AUDIT_TARGETS, row.target) }}<span v-if="row.targetId" class="muted"> #{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140">
        <template #default="{ row }">{{ row.ip || '—' }}</template>
      </el-table-column>
      <el-table-column label="明细" width="90">
        <template #default="{ row }">
          <el-button v-if="row.detail" link type="primary" @click="openDetail(row)">查看</el-button>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无操作记录" /></template>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="reload"
      />
    </div>

    <!-- 明细（右抽屉）：优先结构化展示 before/after，回落 JSON -->
    <el-drawer v-model="detailDrawer" title="操作明细" :size="'var(--mido-drawer-width)'">
      <el-descriptions v-if="current" :column="1" border class="detail__meta">
        <el-descriptions-item label="操作人">{{ current.userName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.ip || '—' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ label(AUDIT_ACTIONS, current.action) }}</el-descriptions-item>
        <el-descriptions-item label="对象">
          {{ label(AUDIT_TARGETS, current.target) }}<span v-if="current.targetId"> #{{ current.targetId }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <template v-if="diffRows.length">
        <h4 class="detail__title">变更前后</h4>
        <el-table :data="diffRows" border size="small">
          <el-table-column prop="field" label="字段" width="120" />
          <el-table-column prop="from" label="变更前" />
          <el-table-column prop="to" label="变更后" />
        </el-table>
      </template>

      <h4 class="detail__title">原始明细</h4>
      <pre class="detail__json">{{ detailJson }}</pre>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { auditLogApi, fetchMembers, AUDIT_MODULES, AUDIT_ACTIONS, AUDIT_TARGETS } from '@/api/org'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const members = ref([])
const range = ref(null)
const query = reactive({ userId: undefined, module: undefined, action: undefined, page: 1, size: 20 })

function label(dict, value) {
  return dict.find((d) => d.value === value)?.label || value || '—'
}

async function load() {
  loading.value = true
  try {
    const res = await auditLogApi.query({
      userId: query.userId || undefined,
      module: query.module || undefined,
      action: query.action || undefined,
      startTime: range.value?.[0] || undefined,
      endTime: range.value?.[1] || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.list || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}
function reload() {
  query.page = 1
  load()
}

const detailDrawer = ref(false)
const current = ref(null)
const detailJson = computed(() => {
  try {
    return JSON.stringify(current.value?.detail, null, 2)
  } catch {
    return String(current.value?.detail)
  }
})
// 结构化 diff：支持 {changes:[{field,from,to}]} 与 {from,to} 两种形态
const diffRows = computed(() => {
  const d = current.value?.detail
  if (!d || typeof d !== 'object') return []
  if (Array.isArray(d.changes)) {
    return d.changes.map((c) => ({ field: c.field, from: fmt(c.from), to: fmt(c.to) }))
  }
  if ('from' in d || 'to' in d) {
    return [{ field: '—', from: fmt(d.from), to: fmt(d.to) }]
  }
  return []
})
function fmt(v) {
  if (v === null || v === undefined) return '—'
  return Array.isArray(v) ? v.join(', ') : String(v)
}
function openDetail(row) {
  current.value = row
  detailDrawer.value = true
}

onMounted(async () => {
  members.value = await fetchMembers()
  load()
})
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
  flex-wrap: wrap;
  gap: var(--mido-space-2);
}
.bar__right {
  display: flex;
  gap: var(--mido-space-2);
  flex-wrap: wrap;
}
.bar__filter {
  width: 160px;
}
.bar__filter--sm {
  width: 120px;
}
.bar__range {
  width: 360px;
}
.muted {
  color: var(--el-text-color-secondary);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
.detail__meta {
  margin-bottom: var(--mido-space-4);
}
.detail__title {
  margin: var(--mido-space-4) 0 var(--mido-space-2);
  font-size: var(--mido-font-size-primary);
  color: var(--el-text-color-primary);
}
.detail__json {
  margin: 0;
  padding: var(--mido-space-4);
  background-color: var(--el-fill-color-light);
  border-radius: var(--mido-radius-md);
  font-family: var(--mido-font-mono);
  font-size: var(--mido-font-size-secondary);
  line-height: var(--mido-line-height-secondary);
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
