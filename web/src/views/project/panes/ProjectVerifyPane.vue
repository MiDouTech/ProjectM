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
      <div class="pv__seg-head">
        <span class="mido-h2">价值验收（NPSS）</span>
        <el-button size="small" @click="configVisible = true">评价方式设置</el-button>
      </div>
      <NpssScoreCard v-if="review" :review="review"
        :stakeholder-name="stakeholderName" :external-ids="externalIds"
        :subject-name="subjectName" @scored="load" />
      <el-empty v-else description="尚未发起价值验收（结案后到期自动发起）" :image-size="60" />
    </div>

    <NpssSubjectConfig v-model="configVisible" :project-id="projectId" @saved="load" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import NpssScoreCard from '@/components/NpssScoreCard.vue'
import NpssSubjectConfig from '@/components/NpssSubjectConfig.vue'
import { npssApi } from '@/api/npss'
import { stakeholderApi } from '@/api/stakeholder'

const props = defineProps({
  project: { type: Object, default: () => ({}) },
  projectId: { type: [Number, String], default: null },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})

const loading = ref(false)
const review = ref(null)
const stakeholders = ref([])
const subjects = ref([])
const configVisible = ref(false)

// 评价主体 id→名称（用于验收 Tab 按主体分组展示得分）
const subjectName = (id) => {
  const s = subjects.value.find((x) => String(x.id) === String(id))
  return s ? s.name : `主体#${id}`
}

const money = (v) => (v == null ? '—' : Number(v).toFixed(2))

// 干系人名称解析 + 外部干系人识别（无系统账号者 userId 为空）
const stakeholderName = (id) => {
  const s = stakeholders.value.find((x) => String(x.id) === String(id))
  if (!s) return `干系人#${id}`
  return s.externalName || props.userName(s.userId)
}
const externalIds = computed(() =>
  stakeholders.value.filter((s) => !s.userId).map((s) => s.id))

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    const [reviews, stks, subs] = await Promise.all([
      npssApi.listByProject(props.projectId),
      stakeholderApi.list(props.projectId),
      npssApi.listProjectSubjects(props.projectId),
    ])
    review.value = reviews && reviews.length ? reviews[0] : null // 取最新一轮
    stakeholders.value = stks || []
    subjects.value = subs || []
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
.pv__seg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.pv__desc {
  margin-top: var(--mido-space-2);
}
</style>
