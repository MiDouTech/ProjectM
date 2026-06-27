<template>
  <div class="mido-page">
    <div class="cp__bar">
      <span class="mido-text-secondary">配置各变更类型「必审/免审」与绑定审批流；必审须绑流，否则提交变更会被拒。</span>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table :data="rows">
        <el-table-column label="变更类型" min-width="160">
          <template #default="{ row }">
            {{ typeLabel(row.changeType) }}
            <span class="mido-mono mido-text-secondary">{{ row.changeType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="必审" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.requireApproval" @change="onToggleApproval(row)" />
          </template>
        </el-table-column>
        <el-table-column label="审批流" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.flowId" :disabled="!row.requireApproval" clearable
              placeholder="选择审批流" class="cp__flow">
              <el-option v-for="f in flows" :key="f.id" :label="f.displayName || f.name" :value="f.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }"><el-switch v-model="row.enabled" /></template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" :loading="row.saving" @click="save(row)">保存</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无变更类型" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePolicyApi, CHANGE_TYPES } from '@/api/change'
import { approvalFlowApi } from '@/api/project'

const loading = ref(false)
const rows = ref([])
const flows = ref([])

const typeLabel = (t) => CHANGE_TYPES.find((x) => x.value === t)?.label || t

async function load() {
  loading.value = true
  try {
    const [policies, flowList] = await Promise.all([
      changePolicyApi.list(),
      approvalFlowApi.list('change'),
    ])
    flows.value = flowList || []
    const byType = Object.fromEntries((policies || []).map((p) => [p.changeType, p]))
    // 以变更类型字典为准，合并已配策略；无策略者默认免审、启用
    rows.value = CHANGE_TYPES.map((t) => {
      const p = byType[t.value]
      return {
        changeType: t.value,
        requireApproval: !!(p && p.requireApproval),
        flowId: p ? p.flowId : null,
        enabled: p ? p.enabled !== 0 : true,
        saving: false,
      }
    })
  } finally {
    loading.value = false
  }
}

// 关闭必审时清掉已绑审批流（与后端 save 语义一致）
function onToggleApproval(row) {
  if (!row.requireApproval) row.flowId = null
}

async function save(row) {
  if (row.requireApproval && !row.flowId) {
    ElMessage.warning('必审策略须先绑定审批流')
    return
  }
  row.saving = true
  try {
    await changePolicyApi.save({
      changeType: row.changeType,
      requireApproval: row.requireApproval ? 1 : 0,
      flowId: row.requireApproval ? row.flowId : null,
      enabled: row.enabled ? 1 : 0,
    })
    ElMessage.success('已保存')
  } finally {
    row.saving = false
  }
}

onMounted(load)
</script>

<style scoped>
.cp__bar {
  display: flex;
  align-items: baseline;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
  flex-wrap: wrap;
}
.cp__flow {
  width: 100%;
}
</style>
