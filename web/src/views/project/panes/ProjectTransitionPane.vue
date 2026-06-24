<template>
  <div class="trans">
    <div class="trans__current">
      <span class="mido-text-secondary">当前状态</span>
      <StatusTag :status="project.status" />
    </div>
    <div v-if="project.status === '审批中' && approverText" class="trans__appr mido-text-secondary">
      {{ approverText }}
    </div>

    <el-divider />

    <div v-if="canWithdraw" class="trans__withdraw">
      <el-button type="warning" plain @click="doWithdraw">撤回立项申请</el-button>
      <span class="mido-text-secondary">撤回后项目回到「草稿」，可修改后重新提交</span>
    </div>

    <h4 class="mido-h2">可执行流转</h4>
    <p class="mido-text-secondary">仅展示用户可手动执行的流转；注册等系统态由立项审批/系统驱动。</p>
    <div v-if="available.length" class="trans__actions">
      <el-button v-for="t in available" :key="t.value" type="primary" plain @click="doTransition(t)">
        {{ t.label }}
      </el-button>
    </div>
    <el-empty v-else description="当前状态无可手动执行的流转" :image-size="60" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { projectApi, approvalApi, MANUAL_TRANSITIONS } from '@/api/project'
import { useUserStore } from '@/store/user'

const props = defineProps({
  project: { type: Object, required: true },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})
const emit = defineEmits(['transitioned'])

const userStore = useUserStore()

// 当前立项审批实例（仅审批中时拉取），用于展示待谁审批
const approval = ref(null)
const approverText = computed(() => {
  const a = approval.value
  if (!a) return ''
  const pending = a.pendingApproverIds || []
  const names = pending.map((id) => props.userName(id)).join('、')
  const nodeName = a.currentNodeName || '审批'
  if (!names) return `${nodeName}：等待系统处理`
  if (String(a.currentMode).toLowerCase() === 'and') {
    const total = pending.length + (a.approvedApproverIds?.length || 0)
    return `${nodeName} · 待 ${names} 审批（会签 ${a.approvedApproverIds?.length || 0}/${total}）`
  }
  return `${nodeName} · 待 ${names} 审批（或签）`
})

// 监听状态而非仅 onMounted：组件被复用（无 :key）时，草稿→审批中也能及时拉取
watch(() => props.project.status, async (s) => {
  approval.value = s === '审批中' ? await projectApi.currentApproval(props.project.id) : null
}, { immediate: true })

// 撤回：仅审批中、实例 pending、且当前用户为发起人（applicantId 经全局序列化为字符串）
const canWithdraw = computed(() =>
  props.project.status === '审批中'
  && approval.value?.status === 'pending'
  && approval.value?.applicantId === userStore.userId)

async function doWithdraw() {
  const { value } = await ElMessageBox.prompt('撤回原因（选填）', '撤回立项申请', {
    confirmButtonText: '确认撤回',
    cancelButtonText: '取消',
    inputType: 'textarea',
  })
  await approvalApi.withdraw(approval.value.id, { reason: value || null })
  ElMessage.success('已撤回，项目回到草稿')
  emit('transitioned')
}

const available = computed(() =>
  MANUAL_TRANSITIONS.filter((t) => t.from.includes(props.project.status)))

async function doTransition(t) {
  await ElMessageBox.confirm(`确认执行「${t.label}」？`, '状态流转', { type: 'warning' })
  await projectApi.transition(props.project.id, { targetStatus: t.value })
  ElMessage.success('流转成功')
  emit('transitioned')
}
</script>

<style scoped>
.trans__current {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
}
.trans__appr {
  margin-top: var(--mido-space-1);
}
.trans__withdraw {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
}
.trans__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
}
</style>
