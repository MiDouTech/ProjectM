<template>
  <!-- 任务详情独立页：与抽屉共用 TaskDetailContent，full-width 充分展示，支持直链分享。 -->
  <div class="mido-page tdp">
    <div class="tdp__bar">
      <el-button link :icon="ArrowLeft" @click="goBack">返回任务</el-button>
    </div>
    <el-card shadow="never" class="tdp__card">
      <TaskDetailContent
        :task-id="taskId"
        :project-id="projectId"
        @open="(id) => $router.push(`/project/${projectId}/task/${id}`)"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import TaskDetailContent from '@/components/TaskDetailContent.vue'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.projectId)
const taskId = computed(() => route.params.taskId)

function goBack() {
  router.push(`/project/${projectId.value}/tasks`)
}
</script>

<style scoped>
.tdp__bar {
  margin-bottom: var(--mido-space-3);
}
.tdp__card {
  /* 给详情内容足够高度撑开左右两栏 */
  min-height: calc(100vh - 160px);
}
.tdp__card :deep(.td) {
  height: 100%;
}
</style>
