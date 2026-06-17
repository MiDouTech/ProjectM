<template>
  <el-drawer v-model="visible" :size="drawerSize" :with-header="false" @open="onOpen">
    <div v-loading="loading" class="pd">
      <!-- 头部（§7-B：标题 + 状态 + 负责人 + 起止时间） -->
      <header class="pd__head">
        <div class="pd__title">
          <CategoryBadge :category="project.category" :show-label="false" />
          <h2 class="mido-h2">{{ project.name }}</h2>
          <StatusTag :status="project.status" />
        </div>
        <div class="pd__meta mido-text-secondary">
          <span class="mido-mono">{{ project.code || '—' }}</span>
          <span>负责人：{{ userName(project.leaderId) }}</span>
          <span>{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</span>
        </div>
      </header>

      <div class="pd__body">
        <!-- 左：信息 / 任务 / 干系人 / 验收 / 文件 -->
        <section class="pd__main">
          <el-tabs v-model="tab">
            <el-tab-pane label="信息" name="info">
              <ProjectInfoPane :project="project" :members="members" :user-name="userName"
                @updated="reloadProject" @members-changed="loadMembers" />
            </el-tab-pane>
            <el-tab-pane label="任务" name="task">
              <el-empty description="在任务工作区管理本项目的看板与任务列表">
                <el-button type="primary" :icon="Operation" @click="goTasks">打开任务工作区</el-button>
              </el-empty>
            </el-tab-pane>
            <el-tab-pane label="干系人" name="stakeholder">
              <el-empty description="在干系人页登记、绘制权力利益矩阵、校验 NPSS 权重">
                <el-button type="primary" :icon="User" @click="goStakeholders">打开干系人页</el-button>
              </el-empty>
            </el-tab-pane>
            <el-tab-pane label="验收" name="verify">
              <el-empty description="NPSS 两段式价值验收在后续验收模块前端接入" />
            </el-tab-pane>
            <el-tab-pane label="甘特图" name="gantt" lazy>
              <GanttChart :project-id="projectId" />
            </el-tab-pane>
            <el-tab-pane label="费用管理" name="cost">
              <CostPanel :project-id="projectId" />
            </el-tab-pane>
            <el-tab-pane label="文件" name="doc">
              <ProjectFilesPanel :project-id="projectId" :user-name="userName" />
            </el-tab-pane>
          </el-tabs>
        </section>

        <!-- 右：评论 / 活动 / 流转 / 状态审批 -->
        <aside class="pd__side">
          <el-tabs v-model="sideTab">
            <el-tab-pane label="流转" name="transition">
              <ProjectTransitionPane :project="project" @transitioned="reloadProject" />
            </el-tab-pane>
            <el-tab-pane label="状态审批" name="approval">
              <ProjectApprovalPane :project="project" @submitted="reloadProject" />
            </el-tab-pane>
            <el-tab-pane label="评论" name="comment">
              <el-empty description="评论在协作模块前端接入" />
            </el-tab-pane>
            <el-tab-pane label="活动" name="activity">
              <ActivityTimeline entity-type="project" :entity-id="projectId" :user-name="userName" />
            </el-tab-pane>
          </el-tabs>
        </aside>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Operation, User } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import { projectApi } from '@/api/project'
import ActivityTimeline from '@/components/ActivityTimeline.vue'
import CostPanel from '@/components/CostPanel.vue'
import ProjectFilesPanel from '@/components/ProjectFilesPanel.vue'
import GanttChart from '@/components/GanttChart.vue'
import ProjectInfoPane from './panes/ProjectInfoPane.vue'
import ProjectTransitionPane from './panes/ProjectTransitionPane.vue'
import ProjectApprovalPane from './panes/ProjectApprovalPane.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: null },
  userMap: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['update:modelValue', 'changed'])

// 详情抽屉比常规抽屉宽（双栏：主 + 活动栏），用两倍 drawer 宽度
const drawerSize = 'calc(var(--mido-drawer-width) * 2)'

const loading = ref(false)
const project = ref({})
const members = ref([])
const tab = ref('info')
const sideTab = ref('transition')

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const router = useRouter()
const userName = (id) => props.userMap[id] || (id ? `用户#${id}` : '—')

function goTasks() {
  visible.value = false
  router.push(`/project/${props.projectId}/tasks`)
}
function goStakeholders() {
  visible.value = false
  router.push(`/project/${props.projectId}/stakeholders`)
}

async function onOpen() {
  tab.value = 'info'
  sideTab.value = 'transition'
  await reloadProject()
  await loadMembers()
}
async function reloadProject() {
  if (!props.projectId) return
  loading.value = true
  try {
    project.value = await projectApi.get(props.projectId)
  } finally {
    loading.value = false
  }
  emit('changed')
}
async function loadMembers() {
  if (!props.projectId) return
  members.value = await projectApi.members(props.projectId)
}
</script>

<style scoped>
.pd {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.pd__head {
  padding-bottom: var(--mido-space-3);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.pd__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.pd__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
  margin-top: var(--mido-space-2);
}
.pd__body {
  display: flex;
  gap: var(--mido-space-4);
  flex: 1;
  min-height: 0;
  margin-top: var(--mido-space-3);
}
.pd__main {
  flex: 1;
  min-width: 0;
}
.pd__side {
  width: var(--mido-drawer-width);
  flex: none;
  border-left: var(--mido-border-width) solid var(--el-border-color-light);
  padding-left: var(--mido-space-4);
}
</style>
