<template>
  <div class="mido-page doc">
    <h1 class="mido-h1">文档中心</h1>
    <p class="mido-text-secondary doc__sub">汇总我参与的各项目文件（项目文档 + 任务/费用附件）。</p>

    <div class="doc__body" v-loading="projLoading">
      <!-- 左：我参与的项目 -->
      <el-card shadow="never" class="doc__side">
        <h3 class="mido-h2">我的项目（{{ projects.length }}）</h3>
        <el-menu v-if="projects.length" :default-active="String(activeId)" @select="selectProject">
          <el-menu-item v-for="p in projects" :key="p.id" :index="String(p.id)">
            <span class="doc__proj">
              <CategoryBadge :category="p.category" />
              <span class="doc__proj-name">{{ p.name }}</span>
            </span>
          </el-menu-item>
        </el-menu>
        <el-empty v-else description="你尚未参与任何项目" :image-size="60" />
      </el-card>

      <!-- 右：选中项目的文件 -->
      <el-card shadow="never" class="doc__main" v-loading="fileLoading">
        <h3 class="mido-h2">{{ activeName || '项目文件' }}（{{ files.length }}）</h3>
        <el-table v-if="activeId" :data="files" size="small">
          <el-table-column label="文件" min-width="220">
            <template #default="{ row }">{{ row.name }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">{{ sourceLabel(row.entityType) }}</template>
          </el-table-column>
          <el-table-column label="大小" width="100" align="right">
            <template #default="{ row }"><span class="mido-mono">{{ fmtSize(row.size) }}</span></template>
          </el-table-column>
          <el-table-column label="上传时间" width="120">
            <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="download(row)">下载</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="该项目暂无文件" :image-size="60" /></template>
        </el-table>
        <el-empty v-else description="从左侧选择一个项目查看其文件" :image-size="60" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CategoryBadge from '@/components/CategoryBadge.vue'
import { projectApi } from '@/api/project'
import { attachmentApi } from '@/api/attachment'

const projLoading = ref(false)
const fileLoading = ref(false)
const projects = ref([])
const files = ref([])
const activeId = ref(null)
const activeName = ref('')

// 附件来源（entityType）→ 中文
const SOURCE_LABEL = { project: '项目文档', task: '任务附件', cost: '费用附件' }
const sourceLabel = (t) => SOURCE_LABEL[t] || t
const fmtDate = (v) => (v ? String(v).slice(0, 10) : '—')
const fmtSize = (b) => {
  if (b == null) return '—'
  if (b < 1024) return `${b} B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
  return `${(b / 1024 / 1024).toFixed(1)} MB`
}

async function loadFiles(id) {
  fileLoading.value = true
  try {
    files.value = await attachmentApi.listByProject(id) || []
  } finally {
    fileLoading.value = false
  }
}
function selectProject(id) {
  const p = projects.value.find((x) => String(x.id) === String(id))
  activeId.value = id
  activeName.value = p ? p.name : ''
  loadFiles(id)
}

async function download(row) {
  const url = await attachmentApi.downloadUrl(row.id)
  if (url) window.open(url, '_blank')
  else ElMessage.error('获取下载链接失败')
}

onMounted(async () => {
  projLoading.value = true
  try {
    projects.value = await projectApi.mine() || []
    if (projects.value.length) selectProject(projects.value[0].id)
  } finally {
    projLoading.value = false
  }
})
</script>

<style scoped>
.doc__sub {
  margin: calc(-1 * var(--mido-space-2)) 0 var(--mido-space-4);
}
.doc__body {
  display: flex;
  gap: var(--mido-space-4);
  align-items: flex-start;
}
.doc__side {
  width: var(--mido-drawer-width);
  flex: none;
}
.doc__main {
  flex: 1;
  min-width: 0;
}
.doc__proj {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.doc__proj-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
