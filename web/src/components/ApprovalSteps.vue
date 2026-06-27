<template>
  <!-- 审批流进度（design-system §7-E：el-steps）。
       节点取自审批流定义（GET /approval-flows/{id}.definition），
       当前位置/状态取自审批实例（currentNode / status）。 -->
  <div v-loading="loading">
    <el-alert v-if="rejected" type="error" :closable="false" show-icon class="alert"
      title="审批未通过" description="该立项申请已被驳回，项目不得进入执行态。请修改后重新提交立项。" />
    <el-alert v-else-if="approved" type="success" :closable="false" show-icon class="alert"
      title="审批通过" description="立项审批已通过，项目进入「已注册」，可启动执行。" />
    <el-alert v-else type="warning" :closable="false" show-icon class="alert"
      title="审批进行中" description="立项审批尚未完成。严肃提示：未通过审批不得进入执行态。" />

    <!-- 精致紧凑态(simple)：去掉技术性 description(节点 key)，只留节点名，体量更小 -->
    <el-steps :active="activeIndex" :process-status="processStatus" finish-status="success" simple class="steps">
      <el-step v-for="n in nodes" :key="n.key" :title="n.name" />
    </el-steps>

    <el-empty v-if="!loading && !nodes.length" description="无审批流节点" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { approvalApi, approvalFlowApi } from '@/api/project'

const props = defineProps({
  // 二选一：传 instanceId 自动拉取实例+流程；或直接传 instance/flowId
  instanceId: { type: [Number, String], default: null },
})

const loading = ref(false)
const nodes = ref([])
const instance = ref(null)

// 审批实例状态码（后端 ApprovalInstance：pending/approved/rejected）
const status = computed(() => instance.value?.status || '')
const approved = computed(() => status.value === 'approved')
const rejected = computed(() => status.value === 'rejected')
const processStatus = computed(() => (rejected.value ? 'error' : 'process'))

// 当前节点索引：通过=全部完成；驳回=停在当前；进行中=当前节点
const activeIndex = computed(() => {
  if (approved.value) return nodes.value.length
  const i = nodes.value.findIndex((n) => n.key === instance.value?.currentNode)
  return i < 0 ? 0 : i
})

async function load() {
  if (!props.instanceId) {
    nodes.value = []
    instance.value = null
    return
  }
  loading.value = true
  try {
    const inst = await approvalApi.getInstance(props.instanceId)
    instance.value = inst
    const flow = await approvalFlowApi.get(inst.flowId)
    const def = typeof flow.definition === 'string' ? JSON.parse(flow.definition) : flow.definition
    nodes.value = def?.nodes || []
  } finally {
    loading.value = false
  }
}

watch(() => props.instanceId, load, { immediate: true })
defineExpose({ reload: load, instance })
</script>

<style scoped>
.alert {
  margin-bottom: var(--mido-space-4);
}
.steps {
  margin-top: var(--mido-space-3);
}
/* 流程条整体收小：标题降至辅助字号，更克制精致 */
.steps :deep(.el-step__title) {
  font-size: var(--mido-font-size-secondary);
}
</style>
