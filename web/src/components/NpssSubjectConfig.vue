<template>
  <el-drawer :model-value="modelValue" title="NPSS 评价方式设置" size="640px" :destroy-on-close="true"
    @update:model-value="(v) => emit('update:modelValue', v)" @open="load">
    <div v-loading="loading" class="nsf">
      <el-alert class="nsf__tip" type="info" :closable="false" show-icon
        title="评价规则：各评价主体加权求和（同主体多人先取平均再加权）。启用主体权重合计须=100%，受益方合计须≥50%，每个主体至少 1 名成员。" />

      <!-- 权重合计实时提示 -->
      <div class="nsf__sum">
        <span class="mido-text-secondary">权重合计</span>
        <span class="mido-mono" :class="{ 'nsf__sum-bad': !sumOk }">{{ totalWeight }}%</span>
        <span class="mido-text-secondary">／ 受益方</span>
        <span class="mido-mono" :class="{ 'nsf__sum-bad': !beneficiaryOk }">{{ beneficiaryWeight }}%</span>
      </div>

      <el-table :data="rows" size="small" border>
        <el-table-column label="评价主体" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" placeholder="主体名称" />
          </template>
        </el-table-column>
        <el-table-column label="权重(%)" width="110" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" :controls="false" size="small" class="nsf__w" />
          </template>
        </el-table-column>
        <el-table-column label="受益方" width="80" align="center">
          <template #default="{ row }"><el-switch v-model="row.beneficiary" /></template>
        </el-table-column>
        <el-table-column label="成员(干系人)" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.memberStakeholderIds" multiple collapse-tags size="small"
              placeholder="选择干系人" class="nsf__members">
              <el-option v-for="s in stakeholderOptions" :key="s.id" :label="s.label" :value="s.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column width="56" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-button class="nsf__add" size="small" @click="addRow">+ 新增评价主体</el-button>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
const totalWeight = computed(() => rows.value.reduce((a, r) => a + num(r.weight), 0))
const beneficiaryWeight = computed(() =>
  rows.value.filter((r) => r.beneficiary).reduce((a, r) => a + num(r.weight), 0))
const sumOk = computed(() => Math.abs(totalWeight.value - 100) < 0.01)
const beneficiaryOk = computed(() => beneficiaryWeight.value >= 50)

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
    rows.value = (subjects || []).map((s) => ({
      name: s.name, weight: num(s.weight), beneficiary: !!s.beneficiary,
      memberStakeholderIds: s.memberStakeholderIds || [],
    }))
    if (!rows.value.length) addRow()
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({ name: '', weight: 0, beneficiary: false, memberStakeholderIds: [] })
}
function removeRow(i) {
  rows.value.splice(i, 1)
}

async function save() {
  // 前端预校验（后端仍硬校验）：合计=100、受益方≥50、每主体≥1成员、名称非空
  if (rows.value.some((r) => !r.name || !r.name.trim())) return ElMessage.warning('评价主体名称不能为空')
  if (rows.value.some((r) => !r.memberStakeholderIds.length)) return ElMessage.warning('每个评价主体至少 1 名成员')
  if (!sumOk.value) return ElMessage.warning('评价主体权重合计须为 100%')
  if (!beneficiaryOk.value) return ElMessage.warning('受益方主体权重合计须≥50%')
  saving.value = true
  try {
    await npssApi.saveProjectSubjects(props.projectId, rows.value.map((r, i) => ({
      name: r.name.trim(), weight: r.weight, beneficiary: r.beneficiary,
      sort: i, memberStakeholderIds: r.memberStakeholderIds,
    })))
    ElMessage.success('评价方式已保存')
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
.nsf__sum-bad {
  color: var(--el-color-danger);
  font-weight: var(--mido-font-weight-bold);
}
.nsf__w,
.nsf__members {
  width: 100%;
}
.nsf__add {
  margin-top: var(--mido-space-3);
}
</style>
