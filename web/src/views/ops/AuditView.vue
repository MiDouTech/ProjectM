<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">审计日志</h2>
      <div class="bar__right">
        <el-input v-model="query.target" placeholder="对象类型" clearable class="bar__filter"
          @keyup.enter="reload" @clear="reload" />
        <el-input v-model="query.action" placeholder="动作" clearable class="bar__filter"
          @keyup.enter="reload" @clear="reload" />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column prop="adminName" label="操作人" width="140">
        <template #default="{ row }">{{ row.adminName || '—' }}</template>
      </el-table-column>
      <el-table-column prop="action" label="动作" width="160" />
      <el-table-column label="对象" min-width="180">
        <template #default="{ row }">
          {{ row.target }}<span v-if="row.targetId"> #{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140">
        <template #default="{ row }">{{ row.ip || '—' }}</template>
      </el-table-column>
      <el-table-column label="明细" width="100">
        <template #default="{ row }">
          <el-button v-if="row.detail" link type="primary" @click="openDetail(row)">查看</el-button>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无审计记录" /></template>
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

    <!-- 明细（右抽屉，JSON 展示）-->
    <el-drawer v-model="detailDrawer" title="审计明细" size="var(--mido-drawer-width)">
      <pre class="detail__json">{{ detailJson }}</pre>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { auditApi } from '@/api/ops'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ target: '', action: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const res = await auditApi.query({
      target: query.target || undefined,
      action: query.action || undefined,
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
const detailObj = ref(null)
const detailJson = computed(() => {
  try {
    return JSON.stringify(detailObj.value, null, 2)
  } catch {
    return String(detailObj.value)
  }
})
function openDetail(row) {
  detailObj.value = row.detail
  detailDrawer.value = true
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
  gap: var(--mido-space-2);
}
.bar__filter {
  width: var(--mido-admin-nav-width);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
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
