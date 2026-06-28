<template>
  <div class="mido-page">
    <WorkspaceShell module="doc" />

    <!-- 顶部操作条（对齐项目列表：视图切换 + 项目/类型/收藏 + 搜索） -->
    <div class="dc__bar">
      <ViewSwitcher v-model="view" :views="VIEWS" />
      <div class="dc__bar-right">
        <el-select v-model="projectFilter" clearable placeholder="全部项目" class="dc__quick" filterable>
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="typeFilter" clearable placeholder="全部类型" class="dc__quick">
          <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="favFilter" placeholder="收藏" class="dc__quick">
          <el-option label="全部" value="" />
          <el-option label="已收藏" value="fav" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索标题" clearable class="dc__search" :prefix-icon="Search" />
      </div>
    </div>

    <el-card shadow="never" v-loading="loading">
      <!-- 列表视图：表头可排序（更新时间/标题），客户端排序后再分页 -->
      <el-table v-if="view === 'list'" :data="pagedRows" stripe class="is-clickable"
        :default-sort="{ prop: sortProp, order: sortOrder }" @sort-change="onSortChange" @row-click="openDoc">
        <el-table-column label="标题" prop="title" sortable="custom" min-width="240">
          <template #default="{ row }">
            <span class="dc__title">
              <el-icon class="dc__icon"><component :is="typeIcon(row.type)" /></el-icon>
              <span>{{ row.title }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" min-width="180">
          <template #default="{ row }">
            <span class="dc__proj">
              <CategoryBadge v-if="projectMap[row.projectId]" :category="projectMap[row.projectId].category" :show-label="false" />
              <span>{{ projectName(row.projectId) }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="更新人" width="110">
          <template #default="{ row }">{{ userName(members, row.updateBy) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="160" prop="updateTime" sortable="custom">
          <template #default="{ row }">{{ fmtTime(row.updateTime) }}</template>
        </el-table-column>
        <template #empty>
          <EmptyState :description="emptyText" :image-size="60" />
        </template>
      </el-table>

      <!-- 卡片视图 -->
      <div v-else class="dc__cards">
        <div v-for="row in pagedRows" :key="row.id" class="dc__card" @click="openDoc(row)">
          <div class="dc__card-top">
            <span class="dc__title">
              <el-icon class="dc__icon"><component :is="typeIcon(row.type)" /></el-icon>
              <span class="dc__card-title">{{ row.title }}</span>
            </span>
            <el-icon v-if="row.favorited" class="dc__fav"><StarFilled /></el-icon>
          </div>
          <div class="dc__proj mido-text-secondary">
            <CategoryBadge v-if="projectMap[row.projectId]" :category="projectMap[row.projectId].category" :show-label="false" />
            <span>{{ projectName(row.projectId) }}</span>
          </div>
          <div class="dc__card-foot mido-text-secondary">
            <span>{{ userName(members, row.updateBy) }}</span>
            <span>{{ fmtTime(row.updateTime) }}</span>
          </div>
        </div>
        <EmptyState v-if="!pagedRows.length" :description="emptyText" :image-size="60" />
      </div>

      <div class="dc__pager">
        <el-pagination layout="total, prev, pager, next" :total="total"
          :current-page="page" :page-size="size" @current-change="(p) => (page = p)" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, Folder, Paperclip, StarFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import EmptyState from '@/components/EmptyState.vue'
import { projectApi } from '@/api/project'
import { docApi } from '@/api/doc'
import { fetchMembers } from '@/api/org'
import { userName } from '@/utils/display'

const router = useRouter()

const VIEWS = [
  { value: 'list', label: '列表' },
  { value: 'card', label: '卡片' },
]
const TYPE_OPTIONS = [
  { value: 'doc', label: '文档' },
  { value: 'file', label: '文件' },
]

const loading = ref(false)
const projects = ref([])
const members = ref([])
const rows = ref([]) // 跨项目全部可读文档（一次拉取，客户端筛选/排序/分页）

const view = ref('list')
const projectFilter = ref('')
const typeFilter = ref('')
const favFilter = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const sortProp = ref('updateTime')
const sortOrder = ref('descending')

const projectMap = computed(() => Object.fromEntries(projects.value.map((p) => [p.id, p])))
const projectName = (id) => projectMap.value[id]?.name || (id ? `项目#${id}` : '—')
const typeLabel = (t) => (t === 'file' ? '文件' : t === 'folder' ? '目录' : '文档')
const typeIcon = (t) => (t === 'file' ? Paperclip : t === 'folder' ? Folder : Document)
const fmtTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—')
const emptyText = computed(() => (!projects.value.length
  ? '你尚未参与任何项目'
  : rows.value.length ? '无符合筛选的文档' : '暂无文档'))

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rows.value.filter((r) => {
    if (projectFilter.value && r.projectId !== projectFilter.value) return false
    if (typeFilter.value && r.type !== typeFilter.value) return false
    if (favFilter.value === 'fav' && !r.favorited) return false
    if (kw && !`${r.title || ''}`.toLowerCase().includes(kw)) return false
    return true
  })
})
const total = computed(() => filteredRows.value.length)
// 客户端排序：先按表头排序整份筛选结果，再切片分页
const sortedRows = computed(() => {
  const arr = [...filteredRows.value]
  if (!sortProp.value) return arr
  const dir = sortOrder.value === 'ascending' ? 1 : -1
  return arr.sort((a, b) => compareBy(a, b, sortProp.value) * dir)
})
const pagedRows = computed(() => sortedRows.value.slice((page.value - 1) * size.value, page.value * size.value))
function compareBy(a, b, prop) {
  const va = a[prop]
  const vb = b[prop]
  if (va == null && vb == null) return 0
  if (va == null) return -1
  if (vb == null) return 1
  return String(va).localeCompare(String(vb)) // updateTime 为 ISO 串、title 为文本，字典序即可
}
function onSortChange({ prop, order }) {
  sortProp.value = order ? prop : 'updateTime'
  sortOrder.value = order || 'descending'
  page.value = 1
}
watch([projectFilter, typeFilter, favFilter, keyword], () => { page.value = 1 })

// 点行下钻：进入该项目知识库工作区并自动选中该文档
function openDoc(row) {
  router.push({ path: '/doc/kb', query: { projectId: row.projectId, doc: row.id } })
}

onMounted(async () => {
  loading.value = true
  try {
    const [proj, mem] = await Promise.all([
      projectApi.mine().catch(() => []),
      fetchMembers().catch(() => []),
    ])
    projects.value = proj || []
    members.value = mem || []
    if (projects.value.length) {
      rows.value = await docApi.list(projects.value.map((p) => p.id)) || []
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dc__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
}
.dc__bar-right {
  display: flex;
  gap: var(--mido-space-2);
}
.dc__quick {
  width: calc(var(--mido-nav-width) * 0.7);
}
.dc__search {
  width: var(--mido-nav-width);
}
.dc__title {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
  min-width: 0;
}
.dc__icon {
  color: var(--el-text-color-secondary);
}
.dc__proj {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.dc__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--mido-space-3);
}
.dc__card {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.dc__card:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}
.dc__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dc__card-title {
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dc__fav {
  color: var(--el-color-warning);
}
.dc__card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dc__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
</style>
