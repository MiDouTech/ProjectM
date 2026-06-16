<template>
  <!-- 看板（design-system §5.2/§6）：列=工作流状态，卡可拖拽改状态。
       :move 据工作流合法流转表预判，禁止非法落列；最终以后端校验为准。 -->
  <div class="kb">
    <div v-for="col in columns" :key="col.status" class="kb__col">
      <header class="kb__head">
        <span class="kb__status"><StatusTag :status="col.status" /></span>
        <span class="kb__count">{{ col.tasks.length }}</span>
      </header>
      <draggable :list="col.tasks" :group="group" item-key="id" :data-status="col.status"
        :move="onMove" :animation="150" ghost-class="kb__ghost" class="kb__list"
        @change="(e) => onChange(e, col.status)">
        <template #item="{ element }">
          <div class="kb__card" @click="$emit('open', element)">
            <slot name="card" :task="element">{{ element.title }}</slot>
          </div>
        </template>
      </draggable>
      <el-empty v-if="!col.tasks.length" :image-size="48" description="拖卡到此" />
    </div>
  </div>
</template>

<script setup>
import draggable from 'vuedraggable'
import StatusTag from '@/components/StatusTag.vue'
import { TASK_TRANSITIONS } from '@/api/task'

defineProps({
  // [{ status, tasks: [] }]，由调用方持有并允许 vuedraggable 原地变更
  columns: { type: Array, required: true },
  group: { type: String, default: 'kanban' },
})
const emit = defineEmits(['change', 'open'])

// 仅允许同列重排，或符合工作流合法流转的跨列移动
function onMove(evt) {
  const from = evt.from?.dataset?.status
  const to = evt.to?.dataset?.status
  if (!to || from === to) return true
  const cur = evt.draggedContext?.element?.status
  return (TASK_TRANSITIONS[cur] || []).includes(to)
}

function onChange(e, toStatus) {
  if (e.added) {
    emit('change', { task: e.added.element, toStatus })
  }
}
</script>

<style scoped>
.kb {
  display: flex;
  gap: var(--mido-space-3);
  align-items: flex-start;
  overflow-x: auto;
  padding-bottom: var(--mido-space-2);
}
.kb__col {
  flex: 0 0 var(--mido-nav-width);
  background-color: var(--el-fill-color-light);
  border-radius: var(--mido-radius-md);
  padding: var(--mido-space-2);
}
.kb__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--mido-space-1) var(--mido-space-1) var(--mido-space-2);
}
.kb__count {
  color: var(--el-text-color-secondary);
  font-size: var(--mido-font-size-caption);
}
.kb__list {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  min-height: var(--mido-space-6);
}
.kb__card {
  background-color: var(--el-bg-color);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-sm);
  padding: var(--mido-space-3);
  cursor: pointer;
  box-shadow: var(--mido-shadow-card);
}
.kb__card:hover {
  border-color: var(--el-color-primary);
}
.kb__ghost {
  opacity: .5;
}
</style>
