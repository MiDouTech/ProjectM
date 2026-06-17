<template>
  <div v-loading="loading">
    <!-- ① 结果验收（铁三角：时间-成本-范围/状态） -->
    <div class="pv__seg">
      <span class="mido-h2">结果验收（铁三角）</span>
      <el-descriptions :column="1" border size="small" class="pv__desc">
        <el-descriptions-item label="时间">
          {{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="成本">
          <span class="mido-mono">{{ money(project.actualCost) }}</span>
          <span class="mido-text-secondary"> / 预算 </span>
          <span class="mido-mono">{{ money(project.budget) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="project.status" /></el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- ② 价值验收（NPSS） -->
    <div class="pv__seg">
      <span class="mido-h2">价值验收（NPSS）</span>
      <NpssScoreCard v-if="review" :review="review" @scored="load" />
      <el-empty v-else description="尚未发起价值验收（结案后到期自动发起）" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import NpssScoreCard from '@/components/NpssScoreCard.vue'
import { npssApi } from '@/api/npss'

const props = defineProps({
  project: { type: Object, default: () => ({}) },
  projectId: { type: [Number, String], default: null },
})

const loading = ref(false)
const review = ref(null)

const money = (v) => (v == null ? '—' : Number(v).toFixed(2))

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    const reviews = await npssApi.listByProject(props.projectId)
    review.value = reviews && reviews.length ? reviews[0] : null // 取最新一轮
  } finally {
    loading.value = false
  }
}

watch(() => props.projectId, load, { immediate: true })
</script>

<style scoped>
.pv__seg {
  margin-bottom: var(--mido-space-5);
}
.pv__desc {
  margin-top: var(--mido-space-2);
}
</style>
