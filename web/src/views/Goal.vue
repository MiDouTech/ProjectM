<template>
  <div class="mido-page">
    <div class="goal__bar">
      <h1 class="mido-h1">目标</h1>
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
        @row-click="openDetail">
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
      </el-descriptions>

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
          <el-table-column label="类型" width="80">
            <template #default="{ row }">{{ row.targetType === 'project' ? '项目' : '任务' }}</template>
          </el-table-column>
          <el-table-column label="对象" prop="targetId" />
          <el-table-column label="操作" width="70">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeAlign(row)">解除</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无对齐" :image-size="50" /></template>
        </el-table>
        <p class="goal__hint mido-text-secondary">项目进度→KR 为只读展示，P1 不自动反写。</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import GoalAlignTree from '@/components/GoalAlignTree.vue'
import UserSelect from '@/components/UserSelect.vue'
import { goalApi, GOAL_TYPES, ALIGN_TARGET_TYPES } from '@/api/goal'
import { fetchMembers } from '@/api/org'
import { userName as nameOf } from '@/utils/display'

const tab = ref('list')
const loading = ref(false)
const saving = ref(false)
const goals = ref([])
const users = ref([])

const createDialog = ref(false)
const formRef = ref()
const form = reactive({ title: '', type: 'objective', parentId: null, ownerId: null, period: '', metricUnit: '', metricStart: null, metricTarget: null, metricCurrent: null })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

const detailOpen = ref(false)
const current = ref({})
const alignments = ref([])
const alignForm = reactive({ targetType: 'project', targetId: null })

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
  Object.assign(form, { title: '', type: 'objective', parentId: null, ownerId: null, period: '', metricUnit: '', metricStart: null, metricTarget: null, metricCurrent: null })
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
}
async function addAlign() {
  if (!alignForm.targetId) {
    ElMessage.warning('请填写对齐对象 ID')
    return
  }
  await goalApi.addAlignment(current.value.id, { targetType: alignForm.targetType, targetId: alignForm.targetId })
  alignForm.targetId = null
  alignments.value = await goalApi.listAlignments(current.value.id)
}
async function removeAlign(row) {
  await goalApi.removeAlignment(row.id)
  alignments.value = await goalApi.listAlignments(current.value.id)
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
</style>
