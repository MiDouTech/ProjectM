<template>
  <div class="pg">
    <div class="pg__head">
      <div>
        <h3 class="mido-h2">对齐到本项目的目标</h3>
        <p class="mido-text-secondary">目标为项目服务：这里展示对齐到本项目的 O / KR，进度为只读。</p>
      </div>
      <el-button type="primary" :icon="Connection" @click="openAlign">对齐目标</el-button>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table v-if="rows.length" :data="rows">
        <el-table-column label="目标 / KR" min-width="240">
          <template #default="{ row }">
            <div class="pg__title">
              <el-tag size="small" :type="row.goal.type === 'objective' ? 'primary' : 'info'" effect="plain">
                {{ typeLabel(row.goal.type) }}
              </el-tag>
              <span>{{ row.goal.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">{{ userName(row.goal.ownerId) }}</template>
        </el-table-column>
        <el-table-column label="周期" width="120">
          <template #default="{ row }">{{ row.goal.period || '—' }}</template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="pct(row.goal.progress)" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="unalign(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="尚无对齐目标，点击「对齐目标」把本项目挂到组织 OKR 之下" :image-size="80" />
    </el-card>

    <!-- 对齐已有目标 -->
    <el-dialog v-model="alignOpen" title="对齐目标到本项目" width="520px">
      <el-select v-model="picked" filterable placeholder="选择要对齐的目标 / KR" class="pg__select">
        <el-option v-for="g in alignable" :key="g.id" :label="`${typeLabel(g.type)} · ${g.title}`" :value="g.id" />
      </el-select>
      <el-empty v-if="!alignable.length" description="没有可对齐的目标，请先在「目标」菜单创建" :image-size="60" />
      <template #footer>
        <el-button @click="alignOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!picked" :loading="saving" @click="confirmAlign">确认对齐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import { goalApi, GOAL_TYPES } from '@/api/goal'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  userName: { type: Function, default: (id) => (id ? `用户#${id}` : '—') },
})

const loading = ref(false)
const rows = ref([])
const allGoals = ref([])
const alignOpen = ref(false)
const picked = ref(null)
const saving = ref(false)

const typeLabel = (t) => GOAL_TYPES.find((x) => x.value === t)?.label || t
const pct = (v) => Math.round(Number(v) || 0)

// 可对齐 = 全部目标 − 已对齐
const alignable = computed(() => {
  const aligned = new Set(rows.value.map((r) => String(r.goal.id)))
  return allGoals.value.filter((g) => !aligned.has(String(g.id)))
})

async function load() {
  loading.value = true
  try {
    rows.value = await goalApi.byProject(props.projectId)
  } finally {
    loading.value = false
  }
}

async function openAlign() {
  allGoals.value = await goalApi.list({})
  picked.value = null
  alignOpen.value = true
}

async function confirmAlign() {
  saving.value = true
  try {
    await goalApi.addAlignment(picked.value, { targetType: 'project', targetId: props.projectId })
    ElMessage.success('对齐成功')
    alignOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function unalign(row) {
  await ElMessageBox.confirm(`解除目标「${row.goal.title}」与本项目的对齐？`, '解除对齐', { type: 'warning' })
  await goalApi.removeAlignment(row.alignmentId)
  ElMessage.success('已解除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.pg__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-3);
}
.pg__title {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.pg__select {
  width: 100%;
}
</style>
