<template>
  <div class="ov">
    <!-- 左：要点 + 关键任务 + 干系人速览 -->
    <section class="ov__main">
      <el-card shadow="never" class="ov__card">
        <h3 class="mido-h2">项目要点</h3>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">{{ categoryLabel(project.category) }}</el-descriptions-item>
          <el-descriptions-item label="子类">{{ project.subCategory || '—' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ userName(project.leaderId) }}</el-descriptions-item>
          <el-descriptions-item label="预算">{{ money(project.budget) }}</el-descriptions-item>
          <el-descriptions-item label="周期">{{ project.startDate || '—' }} ~ {{ project.endDate || '—' }}</el-descriptions-item>
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
          <h3 class="mido-h2">干系人速览（{{ stakeholders.length }}）</h3>
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

    <!-- 右：立项审批 + 生命周期流转引导 -->
    <aside class="ov__side">
      <el-card shadow="never" class="ov__card">
        <h3 class="mido-h2">立项审批</h3>
        <ProjectApprovalPane :project="project" @submitted="$emit('changed')" />
      </el-card>
      <el-card shadow="never" class="ov__card">
        <h3 class="mido-h2">生命周期</h3>
        <ProjectTransitionPane :project="project" @transitioned="$emit('changed')" />
      </el-card>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Flag } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ProjectApprovalPane from './ProjectApprovalPane.vue'
import ProjectTransitionPane from './ProjectTransitionPane.vue'
import { taskApi } from '@/api/task'
import { stakeholderApi } from '@/api/stakeholder'
import { PROJECT_CATEGORIES } from '@/api/project'

const props = defineProps({
  project: { type: Object, required: true },
  projectId: { type: [Number, String], required: true },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})
defineEmits(['changed', 'navigate'])

// 干系人速览仅取前若干，避免拥挤
const STK_PREVIEW = 8

const taskLoading = ref(false)
const tasks = ref([])
const taskTotal = ref(0)
const stkLoading = ref(false)
const stakeholders = ref([])

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
    stakeholders.value = (all || []).slice(0, STK_PREVIEW)
  } finally {
    stkLoading.value = false
  }
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
