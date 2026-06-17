<template>
  <div class="trans">
    <div class="trans__current">
      <span class="mido-text-secondary">当前状态</span>
      <StatusTag :status="project.status" />
    </div>

    <el-timeline class="trans__line">
      <el-timeline-item v-for="s in LIFECYCLE" :key="s"
        :type="dotType(s)" :hollow="s !== project.status">
        <span :class="{ 'trans__now': s === project.status }">{{ s }}</span>
      </el-timeline-item>
    </el-timeline>

    <el-divider />

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
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { projectApi, MANUAL_TRANSITIONS } from '@/api/project'

const props = defineProps({
  project: { type: Object, required: true },
})
const emit = defineEmits(['transitioned'])

// 生命周期顺序（architecture-overview §2.2）
const LIFECYCLE = ['草稿', '审批中', '已注册', '进行中', '结果验收', '已结案', '价值验收中', '已评价']

const doneIndex = computed(() => LIFECYCLE.indexOf(props.project.status))
const dotType = (s) => {
  const i = LIFECYCLE.indexOf(s)
  if (i < doneIndex.value) return 'success'
  if (i === doneIndex.value) return 'primary'
  return 'info'
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
.trans__now {
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-color-primary);
}
.trans__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
}
</style>
