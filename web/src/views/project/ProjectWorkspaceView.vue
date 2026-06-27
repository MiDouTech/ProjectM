<template>
  <div class="mido-page pw" v-loading="loading">
    <!-- 头部（紧凑 2 行）：返回 + 类型 + 标题 + 状态 + 迷你进度 ｜ 主操作 + 更多 -->
    <header class="pw__head">
      <div class="pw__head-main">
        <el-button :icon="ArrowLeft" link @click="$router.push('/project')">全部项目</el-button>
        <CategoryBadge v-if="project.category" :category="project.category" :show-label="false" />
        <h1 class="mido-h1">{{ project.name || '项目' }}</h1>
        <StatusTag v-if="project.status" :status="project.status" />
        <!-- 迷你生命周期进度：N/总 + 细条；hover 展开完整阶段。替代独占一带的大流程条 -->
        <el-popover placement="bottom-start" :width="240" trigger="hover">
          <template #reference>
            <span class="pw__progress" role="button" tabindex="0" aria-label="项目阶段进度">
              <span class="pw__progress-frac mido-text-secondary">{{ activeStage + 1 }}/{{ LIFECYCLE.length }}</span>
              <span class="pw__progress-track"><span class="pw__progress-fill" :style="{ width: progressPct + '%' }" /></span>
            </span>
          </template>
          <el-steps direction="vertical" :active="activeStage" finish-status="success" :space="26">
            <el-step v-for="s in LIFECYCLE" :key="s" :title="s" />
          </el-steps>
        </el-popover>
      </div>
      <div class="pw__head-right">
        <el-button v-for="c in nextSteps" :key="c.label" type="primary" @click="c.run">{{ c.label }}</el-button>
        <el-dropdown v-if="project.id" @command="onHeadCommand">
          <el-button :icon="MoreFilled">更多</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="change" :icon="EditPen">发起变更</el-dropdown-item>
              <el-dropdown-item v-if="canArchive" command="archive" :icon="Box">归档</el-dropdown-item>
              <el-dropdown-item v-if="project.archived === 1" command="unarchive" :icon="RefreshLeft">恢复</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="pw__meta mido-text-secondary">
      <span class="mido-mono">{{ project.code || '—' }}</span>
      <span>负责人：{{ userName(project.leaderId) }}</span>
      <span>{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</span>
    </div>

    <!-- 主体：顶部横向导航 + 全宽内容（详情页统一范式，design-system §7） -->
    <div class="pw__body">
      <div class="pw__tabbar">
        <el-menu :default-active="tab" mode="horizontal" :ellipsis="true" class="pw__nav"
          @select="onSelectTab">
          <el-menu-item v-for="t in navTabs" :key="t.name" :index="t.name">
            <el-icon><component :is="t.icon" /></el-icon>
            <span>{{ t.label }}</span>
          </el-menu-item>
        </el-menu>
        <!-- 齿轮菜单：设置/活动（配置与日志）+ 组件管理，不与浏览导航抢位 -->
        <el-dropdown class="pw__more" trigger="click" @command="onTabbarCommand">
          <el-button link :icon="Setting" aria-label="更多：设置 / 活动 / 组件管理" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="t in menuTabs" :key="t.name" :command="`tab:${t.name}`">
                {{ t.label }}
              </el-dropdown-item>
              <el-dropdown-item command="components" divided>组件管理</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 按 projectId 设 key：切换到另一个项目时强制重挂面板，避免内嵌面板沿用上个项目的数据 -->
      <section class="pw__content" :key="projectId">
        <ProjectOverviewPane v-if="tab === 'overview'" :project="project" :project-id="projectId"
          :user-name="userName" @changed="reload" @navigate="onSelectTab" />
        <ProjectApprovalPane v-else-if="tab === 'approval'" :project="project" @submitted="reload" />
        <ProjectInfoPane v-else-if="tab === 'info'" :project="project" :members="members"
          :user-name="userName" @updated="reload" @members-changed="loadMembers" />
        <TaskWorkspaceView v-else-if="tab === 'task'" :embedded="true" :project-id="projectId" />
        <ProjectGoalsPane v-else-if="tab === 'goal'" :project-id="projectId" :user-name="userName" />
        <StakeholderView v-else-if="tab === 'stakeholder'" :embedded="true" :project-id="projectId" />
        <ProjectVerifyPane v-else-if="tab === 'verify'" :project="project" :project-id="projectId"
          :user-name="userName" @changed="reload" />
        <GanttChart v-else-if="tab === 'gantt'" :project-id="projectId" />
        <CostPanel v-else-if="tab === 'cost'" :project-id="projectId" />
        <ProjectFilesPanel v-else-if="tab === 'doc'" :project-id="projectId" :user-name="userName" />
        <ActivityTimeline v-else-if="tab === 'activity'" entity-type="project"
          :entity-id="projectId" :user-name="userName" />
      </section>
    </div>

    <!-- 组件管理：勾选并排序项目顶栏组件（不勾选任何=回落默认全量 Tab） -->
    <el-dialog v-model="compOpen" title="组件管理" width="var(--mido-login-card-width)">
      <p class="mido-text-secondary">勾选项目需要的组件，可调整顺序；不修改则保持默认。</p>
      <draggable v-model="compSel" item-key="self" handle=".comp__drag" class="comp__list">
        <template #item="{ element }">
          <div class="comp__row">
            <el-icon class="comp__drag"><Rank /></el-icon>
            <el-checkbox :model-value="true" @change="(v) => toggleComp(element, v)">
              {{ tabByName[element]?.label || element }}
            </el-checkbox>
          </div>
        </template>
      </draggable>
      <el-divider>未启用</el-divider>
      <div v-for="t in TABS.filter((x) => !compSel.includes(x.name))" :key="t.name" class="comp__row">
        <el-checkbox :model-value="false" @change="(v) => toggleComp(t.name, v)">{{ t.label }}</el-checkbox>
      </div>
      <template #footer>
        <el-button @click="compOpen = false">取消</el-button>
        <el-button type="primary" @click="saveComp">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发起项目时间变更：受控变更，走变更中心 + 审批引擎（按变更策略必审/免审） -->
    <el-dialog v-model="changeOpen" title="发起项目时间变更" width="var(--mido-login-card-width)">
      <el-form ref="changeRef" :model="changeForm" :rules="changeRules" :label-width="92">
        <el-form-item label="变更事由" prop="reason">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="为什么要调整工期（留痕）" />
        </el-form-item>
        <el-form-item label="影响分析">
          <el-input v-model="changeForm.impact" type="textarea" :rows="2" placeholder="对范围/里程碑/干系人的影响（可选）" />
        </el-form-item>
        <el-divider>拟改值（仅填需变更项）</el-divider>
        <el-form-item label="开始日期">
          <el-date-picker v-model="changeForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="新开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="changeForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="新结束日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeOpen = false">取消</el-button>
        <el-button type="primary" :loading="changeSaving" @click="submitChange">提交变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Odometer, Stamp, InfoFilled, Tickets, Aim, User,
  CircleCheck, TrendCharts, Money, Folder, Clock, Box, RefreshLeft, EditPen, Setting, Rank, MoreFilled,
} from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import ActivityTimeline from '@/components/ActivityTimeline.vue'
import CostPanel from '@/components/CostPanel.vue'
import ProjectFilesPanel from '@/components/ProjectFilesPanel.vue'
import GanttChart from '@/components/GanttChart.vue'
import ProjectOverviewPane from './panes/ProjectOverviewPane.vue'
import ProjectApprovalPane from './panes/ProjectApprovalPane.vue'
import ProjectInfoPane from './panes/ProjectInfoPane.vue'
import ProjectVerifyPane from './panes/ProjectVerifyPane.vue'
import ProjectGoalsPane from './panes/ProjectGoalsPane.vue'
import TaskWorkspaceView from '@/views/task/TaskWorkspaceView.vue'
import StakeholderView from '@/views/stakeholder/StakeholderView.vue'
import { projectApi, componentApi, MANUAL_TRANSITIONS } from '@/api/project'
import { fetchMembers } from '@/api/org'

const LIFECYCLE = ['草稿', '审批中', '已注册', '进行中', '结果验收', '已结案', '价值验收中', '已评价']
const TABS = [
  { name: 'overview', label: '概览', icon: Odometer },
  { name: 'approval', label: '立项', icon: Stamp },
  { name: 'task', label: '任务', icon: Tickets },
  { name: 'goal', label: '目标', icon: Aim },
  { name: 'stakeholder', label: '干系人', icon: User },
  { name: 'verify', label: '验收', icon: CircleCheck },
  { name: 'gantt', label: '甘特图', icon: TrendCharts },
  { name: 'cost', label: '费用', icon: Money },
  { name: 'doc', label: '文件', icon: Folder },
  { name: 'activity', label: '活动', icon: Clock },
  // 「设置」（原「信息」）：基本信息/成员/自定义字段的编辑面，置于末尾与浏览类 Tab 分开
  { name: 'info', label: '设置', icon: InfoFilled },
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

// 阶段5 组件化顶栏：已安装组件驱动可见 Tab；无安装记录则回落默认全量 TABS
const installed = ref([])
const tabByName = Object.fromEntries(TABS.map((t) => [t.name, t]))
// 配置/日志类，收进齿轮菜单（非日常浏览）
const UTILITY_TABS = ['info', 'activity']
// 阶段动作类：仅在相关阶段出现在主导航，平时不占位
const STAGE_TABS = {
  approval: ['草稿', '审批中'],
  verify: ['结果验收', '已结案', '价值验收中', '已评价'],
}
const stageOk = (name) => {
  const allow = STAGE_TABS[name]
  return !allow || allow.includes(project.value.status)
}
// 全量可见集（安装配置优先，否则默认全量）
const installedOrDefault = computed(() => {
  if (!installed.value.length) return TABS
  return installed.value.map((c) => tabByName[c.componentCode]).filter(Boolean)
})
// 主导航：浏览类 + 当前阶段相关的阶段类
const navTabs = computed(() =>
  installedOrDefault.value.filter((t) => !UTILITY_TABS.includes(t.name) && stageOk(t.name)))
// 齿轮菜单：设置 / 活动
const menuTabs = computed(() =>
  installedOrDefault.value.filter((t) => UTILITY_TABS.includes(t.name)))
// 组件管理
const compOpen = ref(false)
const compSel = ref([]) // 选中的组件 code（有序）
async function loadComponents() {
  if (!projectId.value) return
  installed.value = (await componentApi.listInstalled(projectId.value)) || []
  // 当前 Tab 不在可见集合（被卸载/排除）时，切到首个可见 Tab，避免空白内容
  if (!installedOrDefault.value.some((t) => t.name === tab.value)) {
    tab.value = installedOrDefault.value[0]?.name || 'overview'
  }
}
function openComp() {
  // 默认勾选：已安装则用已安装顺序，否则全选(=默认全量)
  compSel.value = installed.value.length ? installed.value.map((c) => c.componentCode) : TABS.map((t) => t.name)
  compOpen.value = true
}
function toggleComp(code, checked) {
  if (checked) {
    if (!compSel.value.includes(code)) compSel.value = [...compSel.value, code]
  } else {
    compSel.value = compSel.value.filter((c) => c !== code)
  }
}
async function saveComp() {
  const payload = compSel.value.map((code, i) => ({
    componentCode: code, name: tabByName[code]?.label, sort: i,
  }))
  await componentApi.saveInstalled(projectId.value, payload)
  ElMessage.success('已保存组件')
  compOpen.value = false
  await loadComponents()
  if (!installedOrDefault.value.some((t) => t.name === tab.value)) {
    tab.value = installedOrDefault.value[0]?.name || 'overview'
  }
}

const activeStage = computed(() => {
  const i = LIFECYCLE.indexOf(project.value.status)
  return i < 0 ? 0 : i
})
// 迷你进度填充百分比：草稿 0% → 已评价 100%
const progressPct = computed(() =>
  LIFECYCLE.length > 1 ? Math.round((activeStage.value / (LIFECYCLE.length - 1)) * 100) : 0)

// 头部「更多」菜单：发起变更 / 归档 / 恢复
function onHeadCommand(cmd) {
  if (cmd === 'change') openChange()
  else if (cmd === 'archive') runArchive()
  else if (cmd === 'unarchive') runUnarchive()
}
// 齿轮菜单：tab:<name> 切到设置/活动；components 打开组件管理
function onTabbarCommand(cmd) {
  if (cmd === 'components') openComp()
  else if (cmd.startsWith('tab:')) onSelectTab(cmd.slice(4))
}

// 下一步 CTA：草稿→提交立项；其余按可手动流转给出（注册等系统态不暴露）
const nextSteps = computed(() => {
  if (project.value.status === '草稿') {
    return [{ label: '提交立项审批', run: () => onSelectTab('approval') }]
  }
  return MANUAL_TRANSITIONS
    .filter((t) => t.from.includes(project.value.status))
    .map((t) => ({ label: t.label, run: () => runTransition(t) }))
})

// 可归档：未归档 且 处于终态（已结案/已评价），对标 Worktile「关闭→归档」
const canArchive = computed(() =>
  project.value.archived !== 1 && ['已结案', '已评价'].includes(project.value.status))

function onSelectTab(name) {
  tab.value = name
}

async function runTransition(t) {
  await ElMessageBox.confirm(`确认执行「${t.label}」？`, '状态流转', { type: 'warning' })
  await projectApi.transition(projectId.value, { targetStatus: t.value })
  ElMessage.success('流转成功')
  await reload()
}

async function runArchive() {
  await ElMessageBox.confirm('归档后项目将从在档列表移除，可在「已归档」中恢复。确认归档？',
    '归档项目', { type: 'warning' })
  await projectApi.archive(projectId.value)
  ElMessage.success('已归档')
  await reload()
}

async function runUnarchive() {
  await projectApi.unarchive(projectId.value)
  ElMessage.success('已恢复')
  await reload()
}

// 发起项目时间变更
const changeOpen = ref(false)
const changeSaving = ref(false)
const changeRef = ref()
const changeForm = reactive({ reason: '', impact: '', startDate: '', endDate: '' })
const changeRules = {
  reason: [{ required: true, message: '请填写变更事由', trigger: 'blur' }],
}
function openChange() {
  Object.assign(changeForm, {
    reason: '', impact: '',
    startDate: project.value.startDate || '', endDate: project.value.endDate || '',
  })
  changeOpen.value = true
}
async function submitChange() {
  await changeRef.value.validate()
  changeSaving.value = true
  try {
    await projectApi.submitChange(projectId.value, { changeType: 'project_schedule', ...changeForm })
    ElMessage.success('变更已提交')
    changeOpen.value = false
    await reload()
  } finally {
    changeSaving.value = false
  }
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
  await Promise.all([reload(), loadMembers(), loadComponents()])
})

// 切换到另一个项目（同组件复用）时重载
watch(projectId, async () => {
  tab.value = 'overview'
  await Promise.all([reload(), loadMembers(), loadComponents()])
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
.pw__head-right {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
/* 迷你生命周期进度：N/总 + 细条，紧凑内联在标题行 */
.pw__progress {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-left: var(--mido-space-2);
  cursor: default;
}
.pw__progress-frac {
  font-size: var(--mido-font-size-caption);
}
.pw__progress-track {
  width: 96px;
  height: var(--mido-space-1);
  border-radius: var(--mido-space-1);
  background: var(--el-fill-color);
  overflow: hidden;
}
.pw__progress-fill {
  display: block;
  height: 100%;
  border-radius: var(--mido-space-1);
  background: var(--el-color-primary);
  transition: width var(--mido-duration) var(--mido-ease);
}
.pw__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
  margin-top: var(--mido-space-2);
  margin-bottom: var(--mido-space-3);
}
.pw__body {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
}
.pw__tabbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.pw__nav {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}
.pw__more {
  flex: none;
  margin-left: var(--mido-space-3);
}
.pw__content {
  flex: 1;
  min-width: 0;
}
.comp__list {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.comp__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-1) 0;
}
.comp__drag {
  cursor: move;
  color: var(--el-text-color-secondary);
}
</style>
