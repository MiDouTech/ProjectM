<template>
  <div class="nsc">
    <!-- 加权汇总 + 结果分级着色 -->
    <div class="nsc__summary">
      <div class="nsc__score">
        <span class="nsc__score-num mido-mono">{{ fmt(review.weightedScore) }}</span>
        <span class="mido-text-secondary">加权满意度</span>
      </div>
      <StatusTag v-if="review.resultLevel" :status="levelLabel(review.resultLevel)" />
      <el-tag v-else type="info" disable-transitions>评分中</el-tag>
    </div>

    <!-- 干系人打分（0-10），待打分行可内联提交 -->
    <el-table :data="review.scores" size="small">
      <el-table-column label="干系人" width="120">
        <template #default="{ row }">{{ stakeholderName(row.stakeholderId) }}</template>
      </el-table-column>
      <el-table-column label="权重" width="80" align="right">
        <template #default="{ row }"><span class="mido-mono">{{ pct(row.weight) }}</span></template>
      </el-table-column>
      <el-table-column label="评分(0-10)" width="180">
        <template #default="{ row }">
          <span v-if="row.score != null" class="mido-mono">{{ row.score }}</span>
          <el-input-number v-else v-model="draft[row.stakeholderId].score" :min="0" :max="10"
            :controls="false" size="small" class="nsc__input" />
        </template>
      </el-table-column>
      <el-table-column label="评价" min-width="200">
        <template #default="{ row }">
          <span v-if="row.score != null">{{ row.comment }}</span>
          <div v-else class="nsc__submit">
            <el-input v-model="draft[row.stakeholderId].comment" size="small" placeholder="评价理由(必填)" />
            <el-button type="primary" size="small" :loading="saving" @click="submit(row)">提交</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from './StatusTag.vue'
import { npssApi, RESULT_LEVEL_LABEL } from '@/api/npss'

const props = defineProps({
  review: { type: Object, required: true },
  // 解析干系人名称（可选）
  stakeholderName: { type: Function, default: (id) => `干系人#${id}` },
})
const emit = defineEmits(['scored'])

const saving = reactive({ value: false })
// 每个待打分干系人的草稿
const draft = reactive({})
watch(() => props.review, (r) => {
  (r?.scores || []).forEach((s) => {
    if (s.score == null && !draft[s.stakeholderId]) {
      draft[s.stakeholderId] = { score: null, comment: '' }
    }
  })
}, { immediate: true, deep: true })

const fmt = (v) => (v == null ? '—' : Number(v).toFixed(2))
const pct = (v) => (v == null ? '—' : `${Number(v)}%`)
const levelLabel = (code) => RESULT_LEVEL_LABEL[code] || code

async function submit(row) {
  const d = draft[row.stakeholderId]
  if (d.score == null) {
    ElMessage.warning('请打 0-10 分')
    return
  }
  if (!d.comment || !d.comment.trim()) {
    ElMessage.warning('请填写评价理由（必填）')
    return
  }
  saving.value = true
  try {
    await npssApi.submitScore(props.review.id, {
      stakeholderId: row.stakeholderId, score: d.score, comment: d.comment,
    })
    ElMessage.success('已提交评分')
    emit('scored')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.nsc__summary {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
  margin-bottom: var(--mido-space-3);
}
.nsc__score {
  display: flex;
  flex-direction: column;
}
.nsc__score-num {
  font-size: var(--mido-font-size-h1);
  font-weight: var(--mido-font-weight-bold);
}
.nsc__submit {
  display: flex;
  gap: var(--mido-space-2);
}
.nsc__input {
  width: calc(var(--mido-admin-nav-width) * 0.6);
}
</style>
