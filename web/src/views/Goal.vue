<template>
  <div class="mido-page">
    <WorkspaceShell module="goal" />
    <div class="goal__bar">
      <div class="goal__bar-actions">
        <el-radio-group v-model="tab">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="graph">对齐网</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建目标</el-button>
      </div>
    </div>

    <!-- 列表/树：量化指标行内编辑 -->
    <el-card v-show="tab === 'list'" shadow="never" v-loading="loading">
      <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all
        class="is-clickable" @row-click="openDetail">
        <el-table-column label="目标" min-width="240">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'objective' ? 'primary' : 'success'" disable-transitions>
              {{ row.type === 'objective' ? '目标' : 'KR' }}
            </el-tag>
            <span class="goal__title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="110">
          <template #default="{ row }">{{ userName(row.ownerId) }}</template>
        </el-table-column>
        <el-table-column label="周期" width="100" prop="period" />
        <el-table-column label="指标(起→当前/目标)" width="240">
          <template #default="{ row }">
            <span class="mido-mono">{{ num(row.metricStart) }} → </span>
            <el-input-number v-model="row.metricCurrent" :precision="2" :controls="false" size="small"
              class="goal__cur" @click.stop @change="(v) => saveMetric(row, v)" />
            <span class="mido-mono"> / {{ num(row.metricTarget) }}</span>
            <span class="mido-text-secondary"> {{ row.metricUnit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }"><el-progress :percentage="pct(row.progress)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click.stop="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无目标，点击右上角新建目标" /></template>
      </el-table>
    </el-card>

    <!-- 对齐网 G6 -->
    <GoalAlignTree v-if="tab === 'graph'" />

    <!-- 新建目标 -->
    <el-dialog v-model="createDialog" title="新建目标/KR" width="var(--mido-login-card-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type">
            <el-option v-for="t in GOAL_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级目标">
          <el-select v-model="form.parentId" clearable placeholder="无（顶层）">
            <el-option v-for="g in goals" :key="g.id" :label="g.title" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <UserSelect v-model="form.ownerId" placeholder="选择负责人" />
        </el-form-item>
        <el-form-item label="周期"><el-input v-model="form.period" placeholder="如 2026Q1" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.metricUnit" placeholder="如 万元 / %" /></el-form-item>
        <el-form-item label="指标">
          <div class="goal__metric">
            <el-input-number v-model="form.metricStart" :precision="2" :controls="false" placeholder="起" />
            <el-input-number v-model="form.metricTarget" :precision="2" :controls="false" placeholder="目标" />
            <el-input-number v-model="form.metricCurrent" :precision="2" :controls="false" placeholder="当前" />
          </div>
        </el-form-item>
        <el-form-item v-if="form.type === 'kr'" label="自动汇总">
          <el-switch v-model="form.autoRollup" :active-value="1" :inactive-value="0" />
          <span class="mido-text-secondary goal__hint">开启后按对齐项目的任务完成率(加权)自动反写本 KR 进度</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情右抽屉：信息 + 对齐 -->
    <el-drawer v-model="detailOpen" :title="current.title" size="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">{{ current.type === 'objective' ? '目标' : 'KR' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ userName(current.ownerId) }}</el-descriptions-item>
        <el-descriptions-item label="周期">{{ current.period || '—' }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ num(current.progress) }}%</el-descriptions-item>
        <el-descriptions-item v-if="current.type === 'kr'" label="自动汇总">
          <el-tag size="small" :type="current.autoRollup === 1 ? 'success' : 'info'" disable-transitions>
            {{ current.autoRollup === 1 ? '开（按对齐项目完成率加权反写）' : '关（手动）' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert v-if="hasPendingChange" type="warning" :closable="false" show-icon class="goal__freeze"
        title="该目标有进行中的变更单，基线已冻结，编辑请走变更流程或先撤回。" />
      <div class="goal__change-bar">
        <el-button :icon="EditPen" :disabled="hasPendingChange" @click="openChange">发起变更</el-button>
        <el-link type="primary" :underline="false" @click="goChangeCenter">变更中心 →</el-link>
      </div>

      <div class="goal__align">
        <span class="mido-h2">对齐（弱关联）</span>
        <div class="goal__align-add">
          <el-select v-model="alignForm.targetType" class="goal__align-type">
            <el-option v-for="t in ALIGN_TARGET_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-input-number v-model="alignForm.targetId" :controls="false" placeholder="对象ID" />
          <el-button type="primary" :icon="Plus" @click="addAlign">对齐</el-button>
        </div>
        <el-table :data="alignments" size="small">
          <el-table-column label="类型" width="70">
            <template #default="{ row }">{{ row.targetType === 'project' ? '项目' : '任务' }}</template>
          </el-table-column>
          <el-table-column label="对象" prop="targetId" />
          <el-table-column label="权重" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.weight" :min="0" :step="1" :precision="2" :controls="false"
                size="small" class="goal__w" @change="(v) => saveWeight(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeAlign(row)">解除</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无对齐" :image-size="50" /></template>
        </el-table>
        <p class="goal__hint mido-text-secondary">
          权重用于多项目汇总到本 KR 时按贡献加权（默认 1=等权）；仅「自动汇总」开启的 KR 生效。
        </p>
      </div>

      <!-- 反向贡献度看板：本 KR 由哪些项目支撑、各自贡献几何 -->
      <div v-if="current.type === 'kr'" class="goal__contrib">
        <div class="goal__contrib-head">
          <span class="mido-h2">支撑项目贡献度</span>
          <span v-if="contribution.items.length" class="mido-text-secondary">
            加权完成率 {{ num(contribution.weightedRate) }}%
          </span>
        </div>
        <template v-if="contribution.items.length">
          <G2Chart :option="contribOption" :height="200" />
          <el-table :data="contribution.items" size="small" class="goal__contrib-tb">
            <el-table-column label="项目" min-width="120">
              <template #default="{ row }">项目#{{ row.projectId }}</template>
            </el-table-column>
            <el-table-column label="完成率" width="150">
              <template #default="{ row }"><el-progress :percentage="pct(row.completionRate)" :stroke-width="8" /></template>
            </el-table-column>
            <el-table-column label="权重" width="70" align="right">
              <template #default="{ row }">{{ num(row.weight) }}</template>
            </el-table-column>
            <el-table-column label="贡献" width="70" align="right">
              <template #default="{ row }"><b>{{ num(row.contribution) }}</b></template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="本 KR 暂无对齐项目" :image-size="50" />
      </div>

      <div class="goal__history">
        <span class="mido-h2">变更历史</span>
        <el-table :data="goalChanges" size="small">
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
          </el-table-column>
          <el-table-column label="事由" prop="reason" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusTag :status="changeStatusLabel(row.status)" /></template>
          </el-table-column>
          <template #empty><el-empty description="暂无变更" :image-size="50" /></template>
        </el-table>
      </div>
    </el-drawer>

    <!-- 发起变更：受控变更，按类型走审批/免审 -->
    <el-dialog v-model="changeOpen" title="发起目标变更" width="var(--mido-login-card-width)">
      <el-form ref="changeRef" :model="changeForm" :rules="changeRules" :label-width="92">
        <el-form-item label="变更类型" prop="changeType">
          <el-select v-model="changeForm.changeType" class="goal__change-type">
            <el-option v-for="t in CHANGE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更事由" prop="reason">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="为什么要变更（留痕）" />
        </el-form-item>
        <el-form-item label="影响分析">
          <el-input v-model="changeForm.impact" type="textarea" :rows="2" placeholder="对范围/进度/干系人的影响（可选）" />
        </el-form-item>
        <el-divider>拟改值（仅填需变更项）</el-divider>
        <el-form-item label="标题"><el-input v-model="changeForm.title" /></el-form-item>
        <el-form-item label="负责人"><UserSelect v-model="changeForm.ownerId" placeholder="选择负责人" /></el-form-item>
        <el-form-item label="周期"><el-input v-model="changeForm.period" placeholder="如 2026Q2" /></el-form-item>
        <el-form-item label="指标目标">
          <el-input-number v-model="changeForm.metricTarget" :precision="2" :controls="false" placeholder="新目标值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeOpen = false">取消</el-button>
        <el-button type="primary" :loading="changeSaving" @click="doSubmitChange">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen } from '@element-plus/icons-vue'
import GoalAlignTree from '@/components/GoalAlignTree.vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import G2Chart from '@/components/G2Chart.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserSelect from '@/components/UserSelect.vue'
import { goalApi, GOAL_TYPES, ALIGN_TARGET_TYPES } from '@/api/goal'
import { CHANGE_TYPES, CHANGE_STATUS } from '@/api/change'
import { fetchMembers } from '@/api/org'
import { userName as nameOf } from '@/utils/display'

const router = useRouter()

const tab = ref('list')
const loading = ref(false)
const saving = ref(false)
const goals = ref([])
const users = ref([])

const createDialog = ref(false)
const formRef = ref()
const form = reactive({ title: '', type: 'objective', parentId: null, ownerId: null, period: '', metricUnit: '', metricStart: null, metricTarget: null, metricCurrent: null, autoRollup: 0 })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

const detailOpen = ref(false)
const current = ref({})
const alignments = ref([])
const alignForm = reactive({ targetType: 'project', targetId: null })
const contribution = ref({ weightedRate: 0, items: [] })
const goalChanges = ref([])
const changeOpen = ref(false)
const changeRef = ref()
const changeSaving = ref(false)
const changeForm = reactive({ changeType: 'goal_target', reason: '', impact: '', title: '', ownerId: null, period: '', metricTarget: null })
const changeRules = {
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写变更事由', trigger: 'blur' }],
}
const hasPendingChange = computed(() => goalChanges.value.some((c) => c.status === 'pending'))
const changeTypeLabel = (t) => CHANGE_TYPES.find((x) => x.value === t)?.label || t
const changeStatusLabel = (s) => CHANGE_STATUS.find((x) => x.value === s)?.label || s

// 贡献度条形图：每个支撑项目对加权完成率的贡献分量（Σ各项=加权完成率）
const contribOption = computed(() => ({
  type: 'interval',
  data: contribution.value.items.map((i) => ({ project: `项目#${i.projectId}`, 贡献: Number(i.contribution) })),
  encode: { x: 'project', y: '贡献', color: 'project' },
  axis: { y: { title: '贡献(加权完成率分量)' } },
  legend: false,
}))

const userName = (id) => nameOf(users.value, id)
const num = (v) => (v == null ? '—' : Number(v))
const pct = (v) => Math.min(100, Math.max(0, Math.round(Number(v || 0))))

// 扁平目标 → parentId 树（parentId 0/空为顶层）
const tree = computed(() => {
  const map = {}
  goals.value.forEach((g) => { map[g.id] = { ...g } })
  const roots = []
  goals.value.forEach((g) => {
    const node = map[g.id]
    if (g.parentId && map[g.parentId]) {
      (map[g.parentId].children ||= []).push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

async function load() {
  loading.value = true
  try {
    goals.value = await goalApi.list({})
  } finally {
    loading.value = false
  }
}

async function saveMetric(row, v) {
  if (v == null) return
  await goalApi.updateMetric(row.id, v)
  ElMessage.success('已更新指标')
  load()
}

function openCreate() {
  Object.assign(form, { title: '', type: 'objective', parentId: null, ownerId: null, period: '', metricUnit: '', metricStart: null, metricTarget: null, metricCurrent: null, autoRollup: 0 })
  formRef.value?.clearValidate()
  createDialog.value = true
}
async function doCreate() {
  await formRef.value.validate()
  saving.value = true
  try {
    await goalApi.create({ ...form })
    ElMessage.success('已创建')
    createDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除目标「${row.title}」?`, '提示', { type: 'warning' })
  await goalApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function openDetail(row) {
  current.value = row
  detailOpen.value = true
  alignments.value = await goalApi.listAlignments(row.id)
  goalChanges.value = await goalApi.changes(row.id)
  await loadContribution()
}

function openChange() {
  const g = current.value
  Object.assign(changeForm, {
    changeType: 'goal_target', reason: '', impact: '',
    title: g.title, ownerId: g.ownerId, period: g.period, metricTarget: g.metricTarget,
  })
  changeRef.value?.clearValidate()
  changeOpen.value = true
}
async function doSubmitChange() {
  await changeRef.value.validate()
  changeSaving.value = true
  try {
    await goalApi.submitChange(current.value.id, { ...changeForm })
    ElMessage.success('变更已提交')
    changeOpen.value = false
    goalChanges.value = await goalApi.changes(current.value.id)
  } finally {
    changeSaving.value = false
  }
}
function goChangeCenter() {
  router.push('/change')
}
async function loadContribution() {
  if (current.value.type !== 'kr') {
    contribution.value = { weightedRate: 0, items: [] }
    return
  }
  contribution.value = await goalApi.contribution(current.value.id)
}
async function addAlign() {
  if (!alignForm.targetId) {
    ElMessage.warning('请填写对齐对象 ID')
    return
  }
  await goalApi.addAlignment(current.value.id, { targetType: alignForm.targetType, targetId: alignForm.targetId })
  alignForm.targetId = null
  alignments.value = await goalApi.listAlignments(current.value.id)
  await loadContribution()
}
async function saveWeight(row, v) {
  if (v == null || v < 0) return
  await goalApi.updateAlignmentWeight(row.id, v)
  ElMessage.success('已更新权重')
  await loadContribution()
  load() // 进度可能因自动汇总变化，刷新列表
}
async function removeAlign(row) {
  await goalApi.removeAlignment(row.id)
  alignments.value = await goalApi.listAlignments(current.value.id)
  await loadContribution()
}

onMounted(async () => {
  users.value = await fetchMembers()
  load()
})
</script>

<style scoped>
.goal__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.goal__bar-actions {
  display: flex;
  gap: var(--mido-space-3);
}
.goal__title {
  margin-left: var(--mido-space-2);
}
.goal__cur {
  width: var(--mido-admin-nav-width);
}
.goal__metric {
  display: flex;
  gap: var(--mido-space-2);
}
.goal__align {
  margin-top: var(--mido-space-5);
}
.goal__align-add {
  display: flex;
  gap: var(--mido-space-2);
  margin: var(--mido-space-3) 0;
}
.goal__align-type {
  width: calc(var(--mido-admin-nav-width) * 0.6);
}
.goal__hint {
  margin-top: var(--mido-space-3);
  font-size: var(--mido-font-size-caption);
}
.goal__w {
  width: calc(var(--mido-admin-nav-width) * 0.6);
}
.goal__contrib {
  margin-top: var(--mido-space-5);
}
.goal__contrib-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.goal__contrib-tb {
  margin-top: var(--mido-space-3);
}
.goal__freeze {
  margin-top: var(--mido-space-3);
}
.goal__change-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--mido-space-3);
}
.goal__change-type {
  width: 100%;
}
.goal__history {
  margin-top: var(--mido-space-5);
}
</style>
