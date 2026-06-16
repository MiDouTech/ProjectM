<template>
  <div class="mido-page">
    <h1 class="mido-h1">工作台</h1>
    <p class="mido-text-secondary">P0 闭环占位页。下方为设计系统基础组件骨架预览。</p>

    <el-card shadow="never" class="mido-page__card">
      <div class="mido-page__row">
        <ViewSwitcher v-model="view" />
      </div>
      <div class="mido-page__row">
        <StatusTag status="进行中" />
        <StatusTag status="有风险" />
        <StatusTag status="逾期" />
        <StatusTag status="已结案" />
        <StatusTag status="未开始" />
      </div>
      <div class="mido-page__row">
        <UserPicker />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import UserPicker from '@/components/UserPicker.vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'

const view = ref('kanban')
</script>

<style scoped>
.mido-page__card {
  margin-top: var(--mido-space-4);
}

.mido-page__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
}

.mido-page__row:last-child {
  margin-bottom: 0;
}
</style>
