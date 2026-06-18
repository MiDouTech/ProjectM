<template>
  <!-- 任务详情抽屉：快速查看/编辑入口。详情内容复用 TaskDetailContent（与独立页同一套）。
       抽屉加宽，主区占比更大；「在新页打开」可切到独立全页。 -->
  <el-drawer v-model="visible" :size="'calc(var(--mido-drawer-width) * 1.85)'" :with-header="false">
    <TaskDetailContent
      v-if="visible"
      :task-id="taskId"
      :project-id="projectId"
      :users="users"
      embedded
      @changed="$emit('changed')"
      @open="(id) => $emit('open', id)"
      @navigate="visible = false"
    />
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import TaskDetailContent from '@/components/TaskDetailContent.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  taskId: { type: [Number, String], default: null },
  projectId: { type: [Number, String], default: null },
  users: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'changed', 'open'])

const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
</script>
