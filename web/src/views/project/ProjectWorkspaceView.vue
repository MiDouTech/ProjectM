<template>
  <div class="mido-page pw" v-loading="loading">
    <!-- 头部：返回 + 标题 + 状态 + 下一步 CTA -->
    <header class="pw__head">
      <div class="pw__head-main">
        <el-button :icon="ArrowLeft" link @click="$router.push('/project')">全部项目</el-button>
        <CategoryBadge v-if="project.category" :category="project.category" :show-label="false" />
        <h1 class="mido-h1">{{ project.name || '项目' }}</h1>
        <StatusTag v-if="project.status" :status="project.status" />
      </div>
      <div v-if="nextSteps.length" class="pw__cta">
        <span class="mido-text-secondary">下一步：</span>
        <el-button v-for="c in nextSteps" :key="c.label" type="primary" @click="c.run">{{ c.label }}</el-button>
      </div>
    </header>
    <div class="pw__meta mido-text-secondary">
      <span class="mido-mono">{{ project.code || '—' }}</span>
      <span>负责人：{{ userName(project.leaderId) }}</span>
      <span>{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</span>
    </div>

    <!-- 生命周期阶段条（常驻引导） -->
    <el-steps :active="activeStage" finish-status="success" align-center class="pw__steps">
      <el-step v-for="s in LIFECYCLE" :key="s" :title="s" />
    </el-steps>

    <!-- 主体：左项目内导航 + 右内容 -->
    <div class="pw__body">
      <el-menu :default-active="tab" class="pw__nav" @select="onSelectTab">
        <el-menu-item v-for="t in TABS" :key="t.name" :index="t.name">
          <el-icon><component :is="t.icon" /></el-icon>
          <span>{{ t.label }}</span>
        </el-menu-item>
      </el-menu>

      <!-- 按 projectId 设 key：切换到另一个项目时强制重挂面板，避免内嵌面板沿用上个项目的数据 -->
      <section class="pw__content" :key="projectId">
        <ProjectOverviewPane v-if="tab === 'overview'" :project="project" :project-id="projectId"
          :user-name="userName" @changed="reload" @navigate="onSelectTab" />
        <ProjectInfoPane v-else-if="tab === 'info'" :project="project" :members="members"
          :user-name="userName" @updated="reload" @members-changed="loadMembers" />
        <TaskWorkspaceView v-else-if="tab === 'task'" :embedded="true" :project-id="projectId" />
        <ProjectGoalsPane v-else-if="tab === 'goal'" :project-id="projectId" :user-name="userName" />
        <StakeholderView v-else-if="tab === 'stakeholder'" :embedded="true" :project-id="projectId" />
        <ProjectVerifyPane v-else-if="tab === 'verify'" :project="project" :project-id="projectId" />
        <GanttChart v-else-if="tab === 'gantt'" :project-id="projectId" />
        <CostPanel v-else-if="tab === 'cost'" :project-id="projectId" />
        <ProjectFilesPanel v-else-if="tab === 'doc'" :project-id="projectId" :user-name="userName" />
        <ActivityTimeline v-else-if="tab === 'activity'" entity-type="project"
          :entity-id="projectId" :user-name="userName" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Odometer, InfoFilled, Tickets, Aim, User,
  CircleCheck, TrendCharts, Money, Folder, Clock,
} from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import ActivityTimeline from '@/components/ActivityTimeline.vue'
import CostPanel from '@/components/CostPanel.vue'
import ProjectFilesPanel from '@/components/ProjectFilesPanel.vue'
import GanttChart from '@/components/GanttChart.vue'
import ProjectOverviewPane from './panes/ProjectOverviewPane.vue'
import ProjectInfoPane from './panes/ProjectInfoPane.vue'
import ProjectVerifyPane from './panes/ProjectVerifyPane.vue'
import ProjectGoalsPane from './panes/ProjectGoalsPane.vue'
import TaskWorkspaceView from '@/views/task/TaskWorkspaceView.vue'
import StakeholderView from '@/views/stakeholder/StakeholderView.vue'
import { projectApi, MANUAL_TRANSITIONS } from '@/api/project'
import { fetchMembers } from '@/api/org'

const LIFECYCLE = ['草稿', '审批中', '已注册', '进行中', '结果验收', '已结案', '价值验收中', '已评价']
const TABS = [
  { name: 'overview', label: '概览', icon: Odometer },
  { name: 'info', label: '信息', icon: InfoFilled },
  { name: 'task', label: '任务', icon: Tickets },
  { name: 'goal', label: '目标', icon: Aim },
  { name: 'stakeholder', label: '干系人', icon: User },
  { name: 'verify', label: '验收', icon: CircleCheck },
  { name: 'gantt', label: '甘特图', icon: TrendCharts },
  { name: 'cost', label: '费用', icon: Money },
  { name: 'doc', label: '文件', icon: Folder },
  { name: 'activity', label: '活动', icon: Clock },
]

const route = useRoute()
const router = useRouter()
// 雪花 ID 为字符串，禁止 Number() 转换（会丢精度）
const projectId = computed(() => route.params.projectId)

const loading = ref(false)
const project = ref({})
const members = ref([])
const users = ref([])
const tab = ref('overview')

const userMap = computed(() => Object.fromEntries(users.value.map((u) => [u.id, u.name])))
const userName = (id) => userMap.value[id] || (id ? `用户#${id}` : '—')

const activeStage = computed(() => {
  const i = LIFECYCLE.indexOf(project.value.status)
  return i < 0 ? 0 : i
})

// 下一步 CTA：草稿→提交立项；其余按可手动流转给出（注册等系统态不暴露）
const nextSteps = computed(() => {
  if (project.value.status === '草稿') {
    return [{ label: '提交立项审批', run: () => onSelectTab('overview') }]
  }
  return MANUAL_TRANSITIONS
    .filter((t) => t.from.includes(project.value.status))
    .map((t) => ({ label: t.label, run: () => runTransition(t) }))
})

function onSelectTab(name) {
  tab.value = name
}

async function runTransition(t) {
  await ElMessageBox.confirm(`确认执行「${t.label}」？`, '状态流转', { type: 'warning' })
  await projectApi.transition(projectId.value, { targetStatus: t.value })
  ElMessage.success('流转成功')
  await reload()
}

async function reload() {
  if (!projectId.value) return
  loading.value = true
  try {
    project.value = await projectApi.get(projectId.value)
  } finally {
    loading.value = false
  }
}
async function loadMembers() {
  if (!projectId.value) return
  members.value = await projectApi.members(projectId.value)
}

onMounted(async () => {
  users.value = await fetchMembers()
  await Promise.all([reload(), loadMembers()])
})

// 切换到另一个项目（同组件复用）时重载
watch(projectId, async () => {
  tab.value = 'overview'
  await Promise.all([reload(), loadMembers()])
})
</script>

<style scoped>
.pw__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  flex-wrap: wrap;
}
.pw__head-main {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.pw__cta {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.pw__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
  margin-top: var(--mido-space-2);
}
.pw__steps {
  margin: var(--mido-space-4) 0;
}
.pw__body {
  display: flex;
  gap: var(--mido-space-4);
  align-items: flex-start;
}
.pw__nav {
  width: var(--mido-admin-nav-width);
  flex: none;
  border-radius: var(--mido-radius-md);
  border: var(--mido-border-width) solid var(--el-border-color-light);
}
.pw__content {
  flex: 1;
  min-width: 0;
}
</style>
