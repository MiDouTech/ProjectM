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
        :move="onMove" :disabled="disabled" :animation="150" ghost-class="kb__ghost" class="kb__list"
        @start="onStart" @end="onEnd" @change="(e) => onChange(e, col.status)">
        <template #item="{ element }">
          <div class="kb__card" @click="onCardClick(element)">
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

const props = defineProps({
  // [{ status, tasks: [] }]，由调用方持有并允许 vuedraggable 原地变更
  columns: { type: Array, required: true },
  group: { type: String, default: 'kanban' },
  // 流转请求在途时禁用拖拽，避免基于陈旧状态的二次拖拽误判
  disabled: { type: Boolean, default: false },
  // 跨列移动合法性（域无关）：调用方注入 (from, to, element) => boolean；不传则放行
  canMove: { type: Function, default: null },
})
const emit = defineEmits(['change', 'open'])

// 同列重排恒允许；跨列由调用方注入的 canMove 决定，保持本组件域无关
function onMove(evt) {
  const from = evt.from?.dataset?.status
  const to = evt.to?.dataset?.status
  if (!to || from === to) return true
  if (!props.canMove) return true
  return props.canMove(from, to, evt.draggedContext?.element)
}

function onChange(e, toStatus) {
  if (e.added) {
    emit('change', { task: e.added.element, toStatus })
  }
}

// 抑制拖拽结束后浏览器补发的 click，避免拖完顺手打开详情抽屉
let dragged = false
function onStart() {
  dragged = false
}
function onEnd() {
  dragged = true
  // click 在同一事件循环紧随 mouseup 触发，置位在本 tick 生效；下一宏任务复位
  setTimeout(() => { dragged = false }, 0)
}
function onCardClick(element) {
  if (dragged) return
  emit('open', element)
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
