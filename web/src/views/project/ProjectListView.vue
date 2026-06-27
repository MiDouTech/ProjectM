<template>
  <div class="mido-page">
    <WorkspaceShell module="project" />
    <!-- 顶部操作条（§7-A：新建 + 视图切换 + 排序 + 筛选 + 搜索） -->
    <div class="pl__bar">
      <div class="pl__bar-left">
        <ViewSwitcher v-model="view" :views="VIEWS" />
      </div>
      <div class="pl__bar-right">
        <el-select v-model="quickCategory" placeholder="类型" clearable class="pl__quick" @change="load">
          <el-option v-for="c in PROJECT_CATEGORIES" :key="c.value" :label="`${c.value} ${c.label}`" :value="c.value" />
        </el-select>
        <el-select v-model="quickStatus" placeholder="状态" clearable class="pl__quick" @change="load">
          <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="archivedView" class="pl__quick" @change="onArchivedChange">
          <el-option label="在档" :value="0" />
          <el-option label="已归档" :value="1" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索名称/编码" clearable class="pl__search"
          :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        <!-- 排序统一在表头切（design-system §7-A）；卡片视图无表头，保留精简排序入口作为例外 -->
        <template v-if="view === 'card'">
          <el-select v-model="sortField" placeholder="排序" class="pl__quick">
            <el-option v-for="f in SORT_FIELDS" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>
          <el-button :icon="sortOrder === 'asc' ? SortUp : SortDown" aria-label="切换排序方向" @click="toggleOrder" />
        </template>
        <el-popover ref="filterPop" placement="bottom-end" :width="'auto'" trigger="click">
          <template #reference>
            <el-badge :value="activeFilter?.rules?.length || 0" :hidden="!activeFilter?.rules?.length">
              <el-button :icon="Filter">筛选</el-button>
            </el-badge>
          </template>
          <FilterBuilder :fields="FILTER_FIELDS" @apply="onApplyFilter" />
        </el-popover>
        <TableColumnSetting v-if="view === 'table'" list-key="projects"
          :all-columns="PROJECT_COLUMNS" :default-columns="PROJECT_DEFAULT_COLS" @change="onColsChange" />
        <el-button type="primary" :icon="Plus" @click="wizardOpen = true">新建项目</el-button>
      </div>
    </div>

    <!-- 主体视图区 -->
    <el-card shadow="never" v-loading="loading">
      <!-- 列表视图：精简固定列 -->
      <el-table v-if="view === 'list'" :data="viewRows" stripe class="is-clickable"
        :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
        <el-table-column label="项目" prop="name" sortable="custom" min-width="220">
          <template #default="{ row }">
            <div class="pl__name">
              <CategoryBadge :category="row.category" :show-label="false" />
              <span>{{ row.name }}</span>
              <span class="mido-mono mido-text-secondary">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">{{ userName(row.leaderId) }}</template>
        </el-table-column>
        <el-table-column label="周期" min-width="180">
          <template #default="{ row }">{{ row.startDate || '—' }} ~ {{ row.endDate || '—' }}</template>
        </el-table-column>
        <template #empty>
          <EmptyState description="还没有项目" action-text="新建项目" :action-icon="Plus" @action="wizardOpen = true" />
        </template>
      </el-table>

      <!-- 表格视图：表头可自定义（列/顺序/冻结，每用户持久化） -->
      <el-table v-else-if="view === 'table'" :data="viewRows" stripe class="is-clickable"
        :default-sort="defaultSort" @sort-change="onSortChange" @row-click="openDetail">
        <el-table-column v-for="key in tableCols" :key="key" :label="colLabel(key)" :prop="key"
          :sortable="SORTABLE_KEYS.includes(key) ? 'custom' : false"
          :width="colWidth(key)" :min-width="colMinWidth(key)" :align="colAlign(key)"
          :fixed="tableFrozen.includes(key) ? 'left' : false">
          <template #default="{ row }">
            <div v-if="key === 'name'" class="pl__name">
              <CategoryBadge :category="row.category" :show-label="false" />
              <span>{{ row.name }}</span>
              <span class="mido-mono mido-text-secondary">{{ row.code }}</span>
            </div>
            <StatusTag v-else-if="key === 'status'" :status="row.status" />
            <span v-else-if="key === 'leaderId'">{{ userName(row.leaderId) }}</span>
            <span v-else-if="key === 'subCategory'">{{ row.subCategory || '—' }}</span>
            <span v-else-if="key === 'budget'">{{ money(row.budget) }}</span>
            <span v-else-if="key === 'startDate'">{{ row.startDate || '—' }}</span>
            <span v-else-if="key === 'endDate'">{{ row.endDate || '—' }}</span>
            <span v-else-if="key === 'code'" class="mido-mono">{{ row.code || '—' }}</span>
            <span v-else>{{ row[key] ?? '—' }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState description="还没有项目" action-text="新建项目" :action-icon="Plus" @action="wizardOpen = true" />
        </template>
      </el-table>

      <!-- 卡片 -->
      <div v-else class="pl__cards">
        <div v-for="row in viewRows" :key="row.id" class="pcard" @click="openDetail(row)">
          <div class="pcard__top">
            <CategoryBadge :category="row.category" />
            <StatusTag :status="row.status" />
          </div>
          <div class="pcard__name">{{ row.name }}</div>
          <div class="mido-mono mido-text-secondary">{{ row.code }}</div>
          <div class="pcard__foot mido-text-secondary">
            <span>{{ userName(row.leaderId) }}</span>
            <span>{{ money(row.budget) }}</span>
          </div>
        </div>
        <EmptyState v-if="!viewRows.length" description="还没有项目" action-text="新建项目"
          :action-icon="Plus" @action="wizardOpen = true" />
      </div>

      <div class="pl__pager">
        <el-pagination layout="total, prev, pager, next" :total="total"
          :current-page="page" :page-size="size" @current-change="onPage" />
      </div>
    </el-card>

    <CreateProjectWizard v-model="wizardOpen" @created="onCreated" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Filter, SortUp, SortDown } from '@element-plus/icons-vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import StatusTag from '@/components/StatusTag.vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import EmptyState from '@/components/EmptyState.vue'
import FilterBuilder from '@/components/FilterBuilder.vue'
import CreateProjectWizard from './CreateProjectWizard.vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import { projectApi, PROJECT_CATEGORIES } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { applyFilter, applySort } from '@/utils/filter'

const VIEWS = [
  { value: 'list', label: '列表' },
  { value: 'table', label: '表格' },
  { value: 'card', label: '卡片' },
]
const STATUSES = ['草稿', '审批中', '已注册', '进行中', '结果验收', '已结案', '价值验收中', '已评价']
const SORT_FIELDS = [
  { value: 'createTime', label: '创建时间' },
  { value: 'name', label: '名称' },
  { value: 'budget', label: '预算' },
  { value: 'startDate', label: '开始日期' },
  { value: 'endDate', label: '结束日期' },
]
// 表头可排序的列 key（与列定义对齐）；createTime 无对应列，仅卡片视图下拉可选
const SORTABLE_KEYS = ['name', 'budget', 'startDate', 'endDate']
// 高级筛选字段（前端对当前结果集求值，见 utils/filter）
const FILTER_FIELDS = [
  { value: 'name', label: '名称', type: 'text' },
  { value: 'code', label: '编码', type: 'text' },
  { value: 'category', label: '类型', type: 'enum', options: PROJECT_CATEGORIES.map((c) => ({ value: c.value, label: `${c.value} ${c.label}` })) },
  { value: 'status', label: '状态', type: 'enum', options: STATUSES.map((s) => ({ value: s, label: s })) },
  { value: 'subCategory', label: '子类', type: 'text' },
  { value: 'budget', label: '预算', type: 'number' },
]

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const view = ref('list')

const quickCategory = ref('')
const quickStatus = ref('')
const archivedView = ref(0)
const keyword = ref('')
const sortField = ref('createTime')
const sortOrder = ref('desc')
const filterPop = ref()
const activeFilter = ref(null)

const wizardOpen = ref(false)
const router = useRouter()

const users = ref([])
const userMap = computed(() => Object.fromEntries(users.value.map((u) => [u.id, u.name])))
const userName = (id) => userMap.value[id] || (id ? `用户#${id}` : '—')
const money = (v) => (v == null ? '—' : `¥${Number(v).toLocaleString()}`)

// 表格视图列定义（表头设置用）。name 必选；width/minWidth/align 决定渲染。
const PROJECT_COLUMNS = [
  { key: 'name', label: '项目', required: true },
  { key: 'status', label: '状态' },
  { key: 'leaderId', label: '负责人' },
  { key: 'subCategory', label: '子类' },
  { key: 'budget', label: '预算' },
  { key: 'startDate', label: '开始时间' },
  { key: 'endDate', label: '截止时间' },
  { key: 'code', label: '项目编号' },
]
const PROJECT_DEFAULT_COLS = ['name', 'status', 'leaderId', 'subCategory', 'budget', 'startDate', 'endDate']
const COL_META = {
  name: { minWidth: 220 },
  status: { width: 110 },
  leaderId: { width: 120 },
  subCategory: { width: 110 },
  budget: { width: 130, align: 'right' },
  startDate: { width: 120 },
  endDate: { width: 120 },
  code: { width: 120 },
}
const tableCols = ref([...PROJECT_DEFAULT_COLS])
const tableFrozen = ref([])
const colLabel = (key) => PROJECT_COLUMNS.find((c) => c.key === key)?.label || key
const colWidth = (key) => COL_META[key]?.width
const colMinWidth = (key) => COL_META[key]?.minWidth
const colAlign = (key) => COL_META[key]?.align
function onColsChange({ columns, frozen }) {
  tableCols.value = columns
  tableFrozen.value = frozen
}

// 表头当前排序态（el-table default-sort 回显）：order 用 ascending/descending
const defaultSort = computed(() => ({
  prop: sortField.value,
  order: sortOrder.value === 'asc' ? 'ascending' : 'descending',
}))

// 服务端粗筛（type/status/keyword）→ 前端高级筛选 + 排序精筛
const viewRows = computed(() => {
  let r = applyFilter(rows.value, activeFilter.value)
  r = applySort(r, sortField.value, sortOrder.value)
  return r
})

async function load() {
  loading.value = true
  try {
    const res = await projectApi.query({
      page: page.value, size: size.value,
      category: quickCategory.value || undefined,
      status: quickStatus.value || undefined,
      keyword: keyword.value || undefined,
      archived: archivedView.value,
    })
    rows.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onPage(p) {
  page.value = p
  load()
}
// 切换在档/已归档：结果集差异大，回到第 1 页
function onArchivedChange() {
  page.value = 1
  load()
}
// 表头点击排序：清空则回落默认（创建时间倒序）；viewRows 计算属性自动重排
function onSortChange({ prop, order }) {
  if (!order) {
    sortField.value = 'createTime'
    sortOrder.value = 'desc'
    return
  }
  sortField.value = prop
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
}
// 卡片视图无表头，保留方向切换
function toggleOrder() {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}
function onApplyFilter(f) {
  activeFilter.value = f
  filterPop.value?.hide()
}
function openDetail(row) {
  router.push(`/project/${row.id}`)
}
function onCreated(id) {
  load()
  if (id) router.push(`/project/${id}`)
}

onMounted(async () => {
  users.value = await fetchMembers()
  load()
})
</script>

<style scoped>
.pl__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
  flex-wrap: wrap;
}
.pl__bar-left {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
}
.pl__bar-right {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  flex-wrap: wrap;
}
.pl__quick {
  width: var(--mido-admin-nav-width);
}
.pl__search {
  width: var(--mido-nav-width);
}
.pl__name {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  cursor: pointer;
}
.pl__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--mido-nav-width), 1fr));
  gap: var(--mido-space-3);
}
.pcard {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: box-shadow var(--mido-duration) var(--mido-ease),
    transform var(--mido-duration) var(--mido-ease),
    border-color var(--mido-duration) var(--mido-ease);
}
.pcard:hover {
  box-shadow: var(--mido-shadow-hover);
  border-color: var(--el-color-primary-light-7);
  transform: translateY(-1px);
}
@media (prefers-reduced-motion: reduce) {
  .pcard {
    transition: none;
  }
  .pcard:hover {
    transform: none;
  }
}
.pcard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pcard__name {
  font-weight: var(--mido-font-weight-bold);
}
.pcard__foot {
  display: flex;
  justify-content: space-between;
  margin-top: var(--mido-space-2);
}
.pl__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
</style>
