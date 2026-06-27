<template>
  <div class="ov">
    <!-- 左：健康概览 + 要点 + 关键任务 + 干系人速览 -->
    <section class="ov__main">
      <el-card shadow="never" class="ov__card" v-loading="healthLoading">
        <h3 class="mido-h2">项目健康</h3>
        <div class="ov__health">
          <div class="hstat">
            <div class="hstat__val">{{ health.completionRate }}<span class="hstat__unit">%</span></div>
            <div class="hstat__label">任务完成率</div>
            <el-progress :percentage="health.completionRate" :show-text="false" :stroke-width="6" class="hstat__bar" />
          </div>
          <div class="hstat">
            <div class="hstat__val">{{ health.done }}<span class="hstat__sub"> / {{ health.total }}</span></div>
            <div class="hstat__label">已完成 / 任务总数</div>
          </div>
          <div class="hstat">
            <div class="hstat__val" :class="{ 'is-danger': health.overdue > 0 }">{{ health.overdue }}</div>
            <div class="hstat__label">逾期任务</div>
          </div>
          <div class="hstat">
            <div class="hstat__val">{{ stakeholderTotal }}</div>
            <div class="hstat__label">干系人</div>
          </div>
          <div class="hstat">
            <div class="hstat__val">{{ health.goalCount }}</div>
            <div class="hstat__label">对齐目标</div>
          </div>
          <div class="hstat">
            <div class="hstat__val mido-mono">{{ money(project.actualCost) }}<span class="hstat__sub"> / {{ money(project.budget) }}</span></div>
            <div class="hstat__label">实际 / 预算</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="ov__card">
        <h3 class="mido-h2">项目要点</h3>
        <DynamicDetail v-if="useDetailConfig" :fields="detailFields" :model-value="detailModel"
          :layout="detailLayout" :user-name="userName" />
        <!-- 仅列与头部不重复的属性（负责人/周期/状态已在页头呈现） -->
        <el-descriptions v-else :column="2" border size="small">
          <el-descriptions-item label="类型">{{ categoryLabel(project.category) }}</el-descriptions-item>
          <el-descriptions-item label="子类">{{ project.subCategory || '—' }}</el-descriptions-item>
          <el-descriptions-item label="预算">{{ money(project.budget) }}</el-descriptions-item>
          <el-descriptions-item label="价值验收(NPSS)">
            <el-tag size="small" :type="requiresNpss ? 'success' : 'info'" effect="plain">
              {{ requiresNpss ? '走 NPSS' : '不走 NPSS' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="ov__card" v-loading="taskLoading">
        <div class="ov__card-head">
          <h3 class="mido-h2">关键任务（{{ taskTotal }}）</h3>
          <el-button link type="primary" @click="$emit('navigate', 'task')">查看全部</el-button>
        </div>
        <el-table v-if="tasks.length" :data="tasks">
          <el-table-column label="任务" min-width="200">
            <template #default="{ row }">
              <span class="ov__task"><el-icon v-if="row.isMilestone"><Flag /></el-icon>{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="负责人" width="110">
            <template #default="{ row }">{{ userName(row.assigneeId) }}</template>
          </el-table-column>
          <el-table-column label="截止" width="110">
            <template #default="{ row }">{{ row.dueDate || '—' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无任务，去「任务」标签拆解项目" :image-size="60" />
      </el-card>

      <el-card shadow="never" class="ov__card" v-loading="stkLoading">
        <div class="ov__card-head">
          <h3 class="mido-h2">干系人速览（{{ stakeholderTotal }}）</h3>
          <el-button link type="primary" @click="$emit('navigate', 'stakeholder')">管理干系人</el-button>
        </div>
        <div v-if="stakeholders.length" class="ov__stk">
          <el-tag v-for="s in stakeholders" :key="s.id" size="small" effect="plain">
            {{ s.externalName || userName(s.userId) }}
          </el-tag>
        </div>
        <el-empty v-else description="尚未登记干系人，NPSS 价值验收的地基" :image-size="60" />
      </el-card>
    </section>

    <!-- 右：当前阶段·下一步（收口阶段入口 + 流转操作） -->
    <aside class="ov__side">
      <el-card shadow="never" class="ov__card">
        <h3 class="mido-h2">当前阶段 · 下一步</h3>
        <div v-if="stageAction" class="ov__stage">
          <span class="mido-text-secondary">{{ stageAction.hint }}</span>
          <el-button type="primary" link @click="$emit('navigate', stageAction.tab)">{{ stageAction.label }} →</el-button>
        </div>
        <ProjectTransitionPane :project="project" :user-name="userName" @transitioned="$emit('changed')" />
      </el-card>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Flag } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import DynamicDetail from '@/components/DynamicDetail.vue'
import ProjectTransitionPane from './ProjectTransitionPane.vue'
import { taskApi } from '@/api/task'
import { goalApi } from '@/api/goal'
import { stakeholderApi } from '@/api/stakeholder'
import { PROJECT_CATEGORIES } from '@/api/project'
import { pageConfigApi } from '@/api/view'
import { fieldDefApi } from '@/api/field'
import { parseFieldOptions } from '@/utils/pageConfig'

const props = defineProps({
  project: { type: Object, required: true },
  projectId: { type: [Number, String], required: true },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})

// L3 detail 模板：项目要点可由页面配置渲染（只读），无配置回落原要点（fail-safe）
const useDetailConfig = ref(false)
const detailFields = ref([])
const detailLayout = ref({ columns: 2 })
const PROJECT_DETAIL_BUILTIN = {
  name: { label: '项目名', type: 'text' },
  description: { label: '描述', type: 'text' },
  leaderId: { label: '负责人', type: 'user' },
  budget: { label: '预算', type: 'number' },
  startDate: { label: '开始时间', type: 'date' },
  endDate: { label: '截止时间', type: 'date' },
}
const detailModel = computed(() => ({ ...(props.project || {}), ...((props.project || {}).customFields || {}) }))
async function loadDetailConfig() {
  try {
    const [cfg, customDefs] = await Promise.all([
      pageConfigApi.get('project', 'detail'),
      fieldDefApi.list('project', true).catch(() => []),
    ])
    const byKey = new Map((customDefs || []).map((d) => [d.fieldKey, d]))
    const fields = (cfg?.fields || []).map((f) => {
      if (f.source === 'builtin' && PROJECT_DETAIL_BUILTIN[f.fieldKey]) {
        const b = PROJECT_DETAIL_BUILTIN[f.fieldKey]
        return { fieldKey: f.fieldKey, label: b.label, type: b.type, group: f.group || '' }
      }
      if (f.source === 'custom' && byKey.has(f.fieldKey)) {
        const d = byKey.get(f.fieldKey)
        return { fieldKey: f.fieldKey, label: d.name, type: d.type, options: parseFieldOptions(d.options), group: f.group || '' }
      }
      return null
    }).filter(Boolean)
    if (fields.length) {
      detailFields.value = fields
      detailLayout.value = cfg.layout || { columns: 2 }
      useDetailConfig.value = true
    } else {
      useDetailConfig.value = false
    }
  } catch {
    useDetailConfig.value = false
  }
}
loadDetailConfig()
defineEmits(['changed', 'navigate'])

// 干系人速览仅取前若干，避免拥挤
const STK_PREVIEW = 8

const taskLoading = ref(false)
const tasks = ref([])
const taskTotal = ref(0)
const stkLoading = ref(false)
const stakeholders = ref([])
const stakeholderTotal = ref(0)

// 项目健康指标：前端按既有端点聚合（任务看板含元类别 → 完成/逾期；目标对齐计数）
const healthLoading = ref(false)
const health = reactive({ total: 0, done: 0, overdue: 0, inProgress: 0, completionRate: 0, goalCount: 0 })
async function loadHealth() {
  healthLoading.value = true
  try {
    const [cols, goals] = await Promise.all([
      taskApi.kanban(props.projectId).catch(() => []),
      goalApi.byProject(props.projectId).catch(() => []),
    ])
    const today = new Date().toISOString().slice(0, 10)
    let total = 0, done = 0, overdue = 0, inProgress = 0
    ;(cols || []).forEach((col) => {
      const meta = col.metaCategory
      ;(col.tasks || []).forEach((t) => {
        total += 1
        if (meta === '已完成') {
          done += 1
        } else {
          if (meta === '进行中') inProgress += 1
          if (t.dueDate && t.dueDate < today) overdue += 1
        }
      })
    })
    Object.assign(health, {
      total, done, overdue, inProgress,
      completionRate: total ? Math.round((done / total) * 100) : 0,
      goalCount: (goals || []).length,
    })
  } finally {
    healthLoading.value = false
  }
}

// 当前阶段快捷入口：把分散的立项/验收入口收口到概览
const STAGE_ACTIONS = {
  草稿: { hint: '项目尚未立项', label: '去立项', tab: 'approval' },
  审批中: { hint: '立项审批进行中', label: '查看立项进度', tab: 'approval' },
  结果验收: { hint: '待录入结果验收（铁三角）', label: '去验收', tab: 'verify' },
  价值验收中: { hint: 'NPSS 价值验收进行中', label: '去验收', tab: 'verify' },
}
const stageAction = computed(() => STAGE_ACTIONS[props.project?.status] || null)

const requiresNpss = computed(() => props.project.requiresNpss !== 0)
const categoryLabel = (c) => {
  const hit = PROJECT_CATEGORIES.find((x) => x.value === c)
  return hit ? `${hit.value} ${hit.label}` : (c || '—')
}
const money = (v) => (v == null ? '—' : `¥${Number(v).toLocaleString()}`)

onMounted(async () => {
  taskLoading.value = true
  try {
    const page = await taskApi.query({ projectId: props.projectId, page: 1, size: 6 })
    tasks.value = page.list || []
    taskTotal.value = page.total || 0
  } finally {
    taskLoading.value = false
  }
  stkLoading.value = true
  try {
    const all = await stakeholderApi.list(props.projectId)
    stakeholderTotal.value = (all || []).length
    stakeholders.value = (all || []).slice(0, STK_PREVIEW)
  } finally {
    stkLoading.value = false
  }
  loadHealth()
})
</script>

<style scoped>
.ov {
  display: flex;
  gap: var(--mido-space-4);
  align-items: flex-start;
}
.ov__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-4);
}
.ov__side {
  width: var(--mido-drawer-width);
  flex: none;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-4);
}
.ov__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
/* 健康指标：自适应小卡网格 */
.ov__health {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--mido-space-4);
  margin-top: var(--mido-space-3);
}
.hstat {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.hstat__val {
  font-size: var(--mido-font-size-h1);
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}
.hstat__val.is-danger {
  color: var(--el-color-danger);
}
.hstat__unit {
  font-size: var(--mido-font-size-body);
  margin-left: 2px;
}
.hstat__sub {
  font-size: var(--mido-font-size-secondary);
  font-weight: 400;
  color: var(--el-text-color-secondary);
}
.hstat__label {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-secondary);
}
.hstat__bar {
  margin-top: var(--mido-space-1);
}
/* 当前阶段快捷入口 */
.ov__stage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2) var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
  background: var(--el-fill-color-lighter);
  border-radius: var(--mido-radius-md);
}
.ov__task {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
}
.ov__stk {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
}
.ov__card h3.mido-h2 {
  margin-top: 0;
  margin-bottom: var(--mido-space-3);
}
</style>
