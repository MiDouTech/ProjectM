<template>
  <div class="appr">
    <!-- 草稿：发起立项申请 -->
    <template v-if="project.status === '草稿'">
      <el-alert class="appr__hint" type="warning" :closable="false" show-icon
        title="尚未立项" description="提交立项申请后项目进入审批流；严肃提示：未通过审批不得进入执行态。" />
      <InitiationForm ref="formRef" :category="project.category" />
      <el-button type="primary" :loading="submitting" class="appr__submit" @click="submit">提交立项申请</el-button>
    </template>

    <!-- 审批中 / 已注册及之后：展示进度 -->
    <template v-else-if="afterApproval">
      <ApprovalSteps v-if="instanceId" :instance-id="instanceId" />
      <template v-else>
        <el-alert class="appr__hint" :type="project.status === '审批中' ? 'warning' : 'success'"
          :closable="false" show-icon
          :title="project.status === '审批中' ? '立项审批进行中' : '立项已通过'"
          :description="project.status === '审批中'
            ? '本会话未持有审批实例号。可在「审批」菜单的待我审批中处理，或输入实例号查看进度。'
            : '立项审批已通过，项目已进入执行生命周期。'" />
        <div class="appr__lookup">
          <el-input v-model="lookupId" placeholder="输入审批实例 ID 查看进度" class="appr__lookup-input" />
          <el-button @click="instanceId = lookupId" :disabled="!lookupId">查看进度</el-button>
        </div>
      </template>
    </template>

    <el-empty v-else description="该状态无关联立项审批" :image-size="60" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import InitiationForm from '../InitiationForm.vue'
import ApprovalSteps from '@/components/ApprovalSteps.vue'
import { projectApi } from '@/api/project'

const props = defineProps({
  project: { type: Object, required: true },
})
const emit = defineEmits(['submitted'])

const formRef = ref()
const submitting = ref(false)
const instanceId = ref(null)
const lookupId = ref('')

// 草稿之后（审批中/已注册/进行中/...）均视为已进入或走过审批
const afterApproval = computed(() => props.project.status !== '草稿')

async function submit() {
  const payload = await formRef.value.validate()
  submitting.value = true
  try {
    instanceId.value = await projectApi.submitApproval(props.project.id, payload)
    ElMessage.success('立项申请已提交')
    emit('submitted')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.appr__hint {
  margin-bottom: var(--mido-space-4);
}
.appr__submit {
  margin-top: var(--mido-space-3);
}
.appr__lookup {
  display: flex;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-4);
}
.appr__lookup-input {
  flex: 1;
}
</style>
