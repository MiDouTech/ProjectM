<template>
  <div v-loading="loading">
    <!-- ① 租户级评价主体模板 -->
    <el-card shadow="never" class="ns__block">
      <div class="bar">
        <h2 class="mido-h2">NPSS 评价主体模板</h2>
        <div class="ns__sum">
          <span class="mido-text-secondary">启用权重合计</span>
          <span class="mido-mono" :class="{ 'ns__bad': !sumOk }">{{ totalWeight }}%</span>
          <span class="mido-text-secondary">／ 受益方</span>
          <span class="mido-mono" :class="{ 'ns__bad': !beneficiaryOk }">{{ beneficiaryWeight }}%</span>
        </div>
      </div>
      <p class="mido-text-secondary tip">
        公司（租户）级评价主体模板，是各项目评价主体/权重的唯一来源；项目内只读、不可改，项目仅配各主体成员（干系人）。
        评价规则：各主体加权求和（同主体多人先平均再加权）。启用主体权重合计须=100%，受益方合计须≥50%。
        模板修改经变更中心受控落库（默认即时生效并留痕；配置审批策略后自动改走审批）。
      </p>

      <el-table :data="rows" stripe border size="small">
        <el-table-column label="评价主体" min-width="160">
          <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="主体名称" /></template>
        </el-table-column>
        <el-table-column label="权重(%)" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" :controls="false" size="small" class="ns__w" />
          </template>
        </el-table-column>
        <el-table-column label="受益方" width="80" align="center">
          <template #default="{ row }"><el-switch v-model="row.beneficiary" /></template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }"><el-switch v-model="row.enabled" /></template>
        </el-table-column>
        <el-table-column width="64" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="rows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无模板，点击下方新增" :image-size="60" /></template>
      </el-table>

      <div class="ns__actions">
        <el-button size="small" @click="addRow">+ 新增评价主体</el-button>
        <el-button type="primary" size="small" :loading="savingTpl" @click="saveTemplates">保存模板</el-button>
      </div>
    </el-card>

    <!-- ② 财年起始月 -->
    <el-card shadow="never" class="ns__block">
      <h2 class="mido-h2">PMO 财年口径</h2>
      <p class="mido-text-secondary tip">
        财年起始月，影响 PMO 总体评价（成功%−失败%）的财年聚合区间。默认 1 月（自然年）；如设 4 则财年 = 当年 4 月 ~ 次年 4 月。
      </p>
      <div class="ns__fiscal">
        <span>财年起始月</span>
        <el-select v-model="fiscalMonth" class="ns__month">
          <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
        </el-select>
        <el-button type="primary" size="small" :loading="savingFiscal" @click="saveFiscal">保存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { npssApi, reportApi } from '@/api/npss'

const loading = ref(false)
const savingTpl = ref(false)
const savingFiscal = ref(false)
const rows = ref([])
const fiscalMonth = ref(1)

const num = (v) => (Number.isFinite(Number(v)) ? Number(v) : 0)
const enabledRows = computed(() => rows.value.filter((r) => r.enabled))
const totalWeight = computed(() => enabledRows.value.reduce((a, r) => a + num(r.weight), 0))
const beneficiaryWeight = computed(() =>
  enabledRows.value.filter((r) => r.beneficiary).reduce((a, r) => a + num(r.weight), 0))
const sumOk = computed(() => Math.abs(totalWeight.value - 100) < 0.01)
const beneficiaryOk = computed(() => beneficiaryWeight.value >= 50)

async function load() {
  loading.value = true
  try {
    const [tpls, settings] = await Promise.all([
      npssApi.listSubjectTemplates(),
      reportApi.getSettings(),
    ])
    rows.value = (tpls || []).map((t) => ({
      name: t.name, weight: num(t.weight), beneficiary: !!t.beneficiary, enabled: !!t.enabled,
    }))
    fiscalMonth.value = settings?.fiscalYearStartMonth || 1
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({ name: '', weight: 0, beneficiary: false, enabled: true })
}

async function saveTemplates() {
  if (enabledRows.value.some((r) => !r.name || !r.name.trim())) return ElMessage.warning('启用的评价主体名称不能为空')
  if (!sumOk.value) return ElMessage.warning('启用主体权重合计须为 100%')
  if (!beneficiaryOk.value) return ElMessage.warning('受益方主体权重合计须≥50%')
  savingTpl.value = true
  try {
    await npssApi.saveSubjectTemplates(rows.value.map((r, i) => ({
      name: r.name.trim(), weight: r.weight, beneficiary: r.beneficiary, enabled: r.enabled, sort: i,
    })))
    ElMessage.success('模板已保存')
    load()
  } finally {
    savingTpl.value = false
  }
}

async function saveFiscal() {
  savingFiscal.value = true
  try {
    await npssApi.saveSettings({ fiscalYearStartMonth: fiscalMonth.value })
    ElMessage.success('财年起始月已保存')
  } finally {
    savingFiscal.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ns__block {
  margin-bottom: var(--mido-space-4);
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.tip {
  margin-bottom: var(--mido-space-4);
}
.ns__sum {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.ns__bad {
  color: var(--el-color-danger);
  font-weight: var(--mido-font-weight-bold);
}
.ns__w {
  width: 100%;
}
.ns__actions {
  display: flex;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-3);
}
.ns__fiscal {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
}
.ns__month {
  width: calc(var(--mido-admin-nav-width) * 0.7);
}
</style>
