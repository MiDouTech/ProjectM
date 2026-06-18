<template>
  <div class="act" v-loading="loading">
    <el-timeline v-if="items.length">
      <el-timeline-item v-for="a in items" :key="a.id" :timestamp="fmtTime(a.createTime)" placement="top">
        <div class="act__line">
          <span class="act__who">{{ who(a.userId) }}</span>
          <span class="act__what">
            <template v-if="a.action === 'created'">创建了{{ entityLabel }}</template>
            <template v-else-if="a.action === 'status_changed'">
              状态
              <StatusTag v-if="a.detail && a.detail.from" :status="a.detail.from" />
              <span v-else>—</span>
              <span class="act__arrow">→</span>
              <StatusTag :status="a.detail && a.detail.to" />
            </template>
            <template v-else-if="a.action === 'assigned'">
              负责人：{{ nameOrEmpty(a.detail && a.detail.from) }}
              <span class="act__arrow">→</span>
              {{ nameOrEmpty(a.detail && a.detail.to) }}
            </template>
            <template v-else-if="a.action === 'updated'">编辑了字段</template>
            <template v-else>{{ a.action }}</template>
          </span>
        </div>
        <ul v-if="a.action === 'updated' && a.detail && a.detail.changes && a.detail.changes.length"
          class="act__changes mido-text-secondary">
          <li v-for="(c, i) in a.detail.changes" :key="i">
            {{ fieldLabel(c.field) }}：{{ fmtVal(c.field, c.from) }}
            <span class="act__arrow">→</span>
            {{ fmtVal(c.field, c.to) }}
          </li>
        </ul>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else-if="!loading" description="暂无活动" />
    <div v-if="items.length < total" class="act__more">
      <el-button link type="primary" :loading="loading" @click="loadMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { projectApi } from '@/api/project'
import { taskApi, TASK_PRIORITIES } from '@/api/task'

const props = defineProps({
  // 'project' | 'task'
  entityType: { type: String, required: true },
  entityId: { type: [Number, String], default: null },
  // 由抽屉传入的人名解析器（复用各抽屉既有 userMap/users）
  userName: { type: Function, required: true },
})

const SIZE = 20
const FIELD_LABELS = {
  name: '名称', subCategory: '子类', leaderId: '负责人', budget: '预算',
  description: '描述', startDate: '开始', endDate: '结束',
  title: '标题', priority: '优先级', stage: '阶段', dueDate: '截止', isMilestone: '里程碑',
}

const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const entityLabel = computed(() => (props.entityType === 'task' ? '任务' : '项目'))
const api = computed(() => (props.entityType === 'task' ? taskApi : projectApi))

const who = (id) => (id ? props.userName(id) : '系统')
const nameOrEmpty = (id) => (id ? props.userName(id) : '空')

function fieldLabel(field) {
  return FIELD_LABELS[field] || field
}
function fmtVal(field, val) {
  if (val == null || val === '') return '空'
  if (field === 'leaderId' || field === 'assigneeId') return props.userName(val)
  if (field === 'budget') return `¥${Number(val).toLocaleString()}`
  if (field === 'priority') return TASK_PRIORITIES.find((p) => p.value === Number(val))?.label || val
  if (field === 'isMilestone') return Number(val) === 1 ? '是' : '否'
  return String(val)
}
// 后端 LocalDateTime 序列化为 ISO 串，取到分钟，T 换空格
function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load(reset) {
  if (!props.entityId) return
  loading.value = true
  try {
    const res = await api.value.activities(props.entityId, { page: page.value, size: SIZE })
    total.value = res.total || 0
    items.value = reset ? (res.list || []) : items.value.concat(res.list || [])
  } finally {
    loading.value = false
  }
}
function loadMore() {
  page.value += 1
  load(false)
}

watch(() => props.entityId, () => {
  page.value = 1
  items.value = []
  total.value = 0
  load(true)
}, { immediate: true })
</script>

<style scoped>
.act__line {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  flex-wrap: wrap;
}
.act__who {
  font-weight: var(--mido-font-weight-bold);
}
.act__what {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
  flex-wrap: wrap;
}
.act__arrow {
  color: var(--el-text-color-secondary);
}
.act__changes {
  margin: var(--mido-space-1) 0 0;
  padding-left: var(--mido-space-4);
}
.act__changes li {
  line-height: var(--mido-line-height-secondary);
}
.act__more {
  text-align: center;
  margin-top: var(--mido-space-2);
}
</style>
