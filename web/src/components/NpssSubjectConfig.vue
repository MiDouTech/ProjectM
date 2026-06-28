<template>
  <el-drawer :model-value="modelValue" title="NPSS 评价方式设置" size="640px" :destroy-on-close="true"
    @update:model-value="(v) => emit('update:modelValue', v)" @open="load">
    <div v-loading="loading" class="nsf">
      <el-alert class="nsf__tip" type="info" :closable="false" show-icon
        title="评价主体与权重为公司（租户）级统一配置，项目内只读；如需调整请前往「管理后台 → NPSS 设置」并走审批。此处仅为各主体配置成员（干系人）。" />

      <!-- 权重合计（取自租户模板，只读展示） -->
      <div class="nsf__sum">
        <span class="mido-text-secondary">权重合计</span>
        <span class="mido-mono">{{ totalWeight }}%</span>
        <span class="mido-text-secondary">／ 受益方</span>
        <span class="mido-mono">{{ beneficiaryWeight }}%</span>
      </div>

      <el-table :data="rows" size="small" border>
        <el-table-column label="评价主体" min-width="140">
          <template #default="{ row }"><span>{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="权重(%)" width="90" align="center">
          <template #default="{ row }"><span class="mido-mono">{{ row.weight }}</span></template>
        </el-table-column>
        <el-table-column label="受益方" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.beneficiary">是</span>
            <span v-else class="mido-text-secondary">否</span>
          </template>
        </el-table-column>
        <el-table-column label="成员(干系人)" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.memberStakeholderIds" multiple collapse-tags size="small"
              placeholder="选择干系人" class="nsf__members">
              <el-option v-for="s in stakeholderOptions" :key="s.id" :label="s.label" :value="s.id" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !rows.length"
        description="尚未配置租户级评价主体，请前往管理后台 → NPSS 设置" :image-size="60" />
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!rows.length" @click="save">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { npssApi } from '@/api/npss'
import { stakeholderApi } from '@/api/stakeholder'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const stakeholderOptions = ref([])

const num = (v) => (Number.isFinite(Number(v)) ? Number(v) : 0)
// 权重合计/受益方合计取自租户模板，仅作只读展示
const totalWeight = computed(() => rows.value.reduce((a, r) => a + num(r.weight), 0))
const beneficiaryWeight = computed(() =>
  rows.value.filter((r) => r.beneficiary).reduce((a, r) => a + num(r.weight), 0))

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    const [subjects, stks] = await Promise.all([
      npssApi.listProjectSubjects(props.projectId),
      stakeholderApi.list(props.projectId),
    ])
    stakeholderOptions.value = (stks || []).map((s) => ({
      id: s.id, label: s.externalName || (s.userId ? `用户#${s.userId}` : `干系人#${s.id}`),
    }))
    // 主体/权重/受益方来自租户模板（只读），成员为项目级（可编辑）
    rows.value = (subjects || []).map((s) => ({
      templateId: s.templateId, name: s.name, weight: num(s.weight), beneficiary: !!s.beneficiary,
      memberStakeholderIds: s.memberStakeholderIds || [],
    }))
  } finally {
    loading.value = false
  }
}

async function save() {
  // 仅校验成员：每个主体≥1 成员（主体/权重由租户模板保证）
  if (rows.value.some((r) => !r.memberStakeholderIds.length)) {
    return ElMessage.warning('每个评价主体至少 1 名成员')
  }
  saving.value = true
  try {
    await npssApi.saveProjectSubjects(props.projectId, rows.value.map((r, i) => ({
      templateId: r.templateId, sort: i, memberStakeholderIds: r.memberStakeholderIds,
    })))
    ElMessage.success('评价成员已保存')
    emit('saved')
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.nsf__tip {
  margin-bottom: var(--mido-space-3);
}
.nsf__sum {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.nsf__members {
  width: 100%;
}
</style>
