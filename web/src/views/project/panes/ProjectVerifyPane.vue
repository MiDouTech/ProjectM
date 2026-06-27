<template>
  <div v-loading="loading">
    <!-- ① 结果验收（铁三角：时间-成本-范围） -->
    <div class="pv__seg">
      <div class="pv__seg-head">
        <span class="mido-h2">结果验收（铁三角）</span>
        <span v-if="latestVerify" :class="['pv__verdict', latestVerify.verdict === 'pass' ? 'is-pass' : 'is-fail']">
          {{ latestVerify.verdict === 'pass' ? '已通过 · 达标' : '不达标' }}
        </span>
      </div>
      <el-descriptions :column="1" border size="small" class="pv__desc">
        <el-descriptions-item label="时间">
          {{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="成本">
          <span class="mido-mono">{{ money(project.actualCost) }}</span>
          <span class="mido-text-secondary"> / 预算 </span>
          <span class="mido-mono">{{ money(project.budget) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="范围">
          任务完成率 <span class="mido-mono">{{ completionText }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="project.status" /></el-descriptions-item>
      </el-descriptions>

      <!-- 处于「结果验收」态：PMO 录入结论。达标(pass)方可结案（后端硬闸门）。 -->
      <div v-if="isResultVerify" class="pv__form">
        <el-form label-width="92px">
          <el-form-item label="三角达标">
            <el-checkbox v-model="form.onTime">时间达标</el-checkbox>
            <el-checkbox v-model="form.inBudget">成本达标</el-checkbox>
            <el-checkbox v-model="form.inScope">范围达标</el-checkbox>
          </el-form-item>
          <el-form-item label="验收结论">
            <el-radio-group v-model="form.verdict">
              <el-radio label="pass">达标</el-radio>
              <el-radio label="fail">不达标</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="1000" show-word-limit
              placeholder="结果验收说明（可选）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitResultVerify">提交结果验收</el-button>
            <el-button v-if="latestVerify && latestVerify.verdict === 'pass'" type="success"
              :loading="closing" @click="confirmClose">确认结案</el-button>
          </el-form-item>
        </el-form>
        <p class="pv__hint mido-text-secondary">
          三角指标已按项目数据自动预填，可调整；结论为「达标」并提交后方可结案。
        </p>
      </div>
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
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import NpssScoreCard from '@/components/NpssScoreCard.vue'
import NpssSubjectConfig from '@/components/NpssSubjectConfig.vue'
import { npssApi, resultVerifyApi, reportApi } from '@/api/npss'
import { projectApi } from '@/api/project'
import { stakeholderApi } from '@/api/stakeholder'

const props = defineProps({
  project: { type: Object, default: () => ({}) },
  projectId: { type: [Number, String], default: null },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})
const emit = defineEmits(['changed'])

const loading = ref(false)
const review = ref(null)
const stakeholders = ref([])
const subjects = ref([])
const configVisible = ref(false)

// 结果验收（铁三角）
const latestVerify = ref(null)
const completionRate = ref(null)
const submitting = ref(false)
const closing = ref(false)
const form = reactive({ verdict: '', onTime: false, inBudget: false, inScope: false, remark: '' })

const isResultVerify = computed(() => props.project?.status === '结果验收')
const completionText = computed(() =>
  completionRate.value == null ? '—' : `${Number(completionRate.value).toFixed(0)}%`)

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

// 三角指标自动预填：时间(今日≤计划截止)/成本(实际≤预算)/范围(完成率≥100%)
function autofillForm() {
  if (latestVerify.value) {
    form.verdict = latestVerify.value.verdict || ''
    form.onTime = !!latestVerify.value.onTime
    form.inBudget = !!latestVerify.value.inBudget
    form.inScope = !!latestVerify.value.inScope
    form.remark = latestVerify.value.remark || ''
    return
  }
  const p = props.project || {}
  // 用本地日期（非 toISOString 的 UTC），否则东八区凌晨会回退到昨天，误判时间达标
  const n = new Date()
  const today = `${n.getFullYear()}-${String(n.getMonth() + 1).padStart(2, '0')}-${String(n.getDate()).padStart(2, '0')}`
  form.onTime = !!p.endDate && today <= p.endDate
  form.inBudget = p.budget != null && Number(p.actualCost || 0) <= Number(p.budget)
  form.inScope = completionRate.value != null && Number(completionRate.value) >= 100
  form.verdict = form.onTime && form.inBudget && form.inScope ? 'pass' : ''
}

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    const [reviews, stks, subs, verify, health] = await Promise.all([
      npssApi.listByProject(props.projectId),
      stakeholderApi.list(props.projectId),
      npssApi.listProjectSubjects(props.projectId),
      resultVerifyApi.latest(props.projectId),
      reportApi.projectHealth(props.projectId).catch(() => null),
    ])
    review.value = reviews && reviews.length ? reviews[0] : null // 取最新一轮
    stakeholders.value = stks || []
    subjects.value = subs || []
    latestVerify.value = verify || null
    completionRate.value = health ? health.completionRate : null
    autofillForm()
  } finally {
    loading.value = false
  }
}

async function submitResultVerify() {
  if (!form.verdict) {
    ElMessage.warning('请选择验收结论')
    return
  }
  submitting.value = true
  try {
    await resultVerifyApi.save(props.projectId, {
      verdict: form.verdict,
      onTime: form.onTime,
      inBudget: form.inBudget,
      inScope: form.inScope,
      completionRate: completionRate.value,
      remark: form.remark,
    })
    ElMessage.success('结果验收已提交')
    await load()
  } finally {
    submitting.value = false
  }
}

async function confirmClose() {
  closing.value = true
  try {
    await projectApi.transition(props.projectId, { targetStatus: '已结案' })
    ElMessage.success('项目已结案')
    emit('changed')
  } finally {
    closing.value = false
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
.pv__form {
  margin-top: var(--mido-space-3);
}
.pv__hint {
  margin: 0;
  font-size: var(--mido-font-size-sm);
}
.pv__verdict {
  font-size: var(--mido-font-size-sm);
  font-weight: 600;
}
.pv__verdict.is-pass {
  color: var(--el-color-success);
}
.pv__verdict.is-fail {
  color: var(--el-color-danger);
}
</style>
