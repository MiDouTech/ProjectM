<template>
  <div class="mido-page">
    <WorkspaceShell module="briefing">
      <template #actions>
        <!-- 全部：写简报（选模板新建）；提交简报：添加模板 -->
        <el-dropdown v-if="activeTab === 'all'" trigger="click" @command="openNewById">
          <el-button type="primary" :icon="Plus">写简报<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="t in templates" :key="t.id" :command="t.id">{{ t.name }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-else-if="activeTab === 'submit'" type="primary" :icon="Plus" @click="openTemplateDialog">添加模板</el-button>
      </template>
    </WorkspaceShell>

    <el-tabs v-model="activeTab" class="bf__tabs bf__tabs--headless">
      <!-- 全部：我的简报（日/周/月合一），类型/状态/关键字筛选 + 视图切换/分页/排序 -->
      <el-tab-pane label="全部" name="all">
        <div class="bf__bar">
          <ViewSwitcher v-model="view" :views="VIEWS" />
          <div class="bf__bar-right">
            <el-select v-model="typeFilter" clearable placeholder="全部类型" class="bf__quick">
              <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-select v-model="statusFilter" clearable placeholder="全部状态" class="bf__quick">
              <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索周期" clearable class="bf__search" :prefix-icon="Search" />
          </div>
        </div>

        <el-card shadow="never" v-loading="loading">
          <el-table v-if="view === 'list'" :data="pagedMine" stripe class="is-clickable"
            :default-sort="{ prop: sortProp, order: sortOrder }" @sort-change="onSortChange" @row-click="openExisting">
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column label="周期" prop="periodKey" sortable="custom" min-width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column label="提交时间" width="170" prop="submittedAt" sortable="custom">
              <template #default="{ row }">{{ row.submittedAt ? fmt(row.submittedAt) : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openExisting(row)">
                  {{ row.status === 'submitted' ? '查看' : '编辑' }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState :description="mineRows.length ? '无符合筛选的简报' : '暂无简报，点右上「写简报」'" :image-size="60" />
            </template>
          </el-table>

          <div v-else class="bf__cards">
            <div v-for="row in pagedMine" :key="row.id" class="bf__card" @click="openExisting(row)">
              <div class="bf__card-top">
                <span class="bf__card-icon" :class="'is-' + row.type">{{ typeShort(row.type) }}</span>
                <StatusTag :status="row.status" />
              </div>
              <div class="bf__card-period">{{ row.periodKey }}</div>
              <div class="bf__card-foot mido-text-secondary">
                <span>{{ typeLabel(row.type) }}</span>
                <span>{{ row.submittedAt ? fmt(row.submittedAt) : '草稿' }}</span>
              </div>
            </div>
            <EmptyState v-if="!pagedMine.length" class="bf__cards-empty"
              :description="mineRows.length ? '无符合筛选的简报' : '暂无简报，点右上「写简报」'" :image-size="60" />
          </div>

          <div class="bf__pager">
            <el-pagination layout="total, prev, pager, next" :total="total"
              :current-page="page" :page-size="size" @current-change="(p) => (page = p)" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 提交简报：模板卡（写入口 + 模板管理） -->
      <el-tab-pane label="提交简报" name="submit" lazy>
        <div class="bf__cards">
          <div v-for="t in templates" :key="t.id" class="mido-card" @click="openNew(t)">
            <div class="mido-card__icon" :class="'is-' + t.type">{{ typeShort(t.type) }}</div>
            <div class="mido-card__name">{{ t.name }}</div>
            <el-dropdown v-if="!t.isBuiltin" trigger="click" @click.stop @command="(cmd) => onTemplateCmd(cmd, t)">
              <el-icon class="mido-card__more" @click.stop><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="assign">指派成员</el-dropdown-item>
                  <el-dropdown-item command="disable">停用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我评审的 -->
      <el-tab-pane label="我评审的" name="review" lazy>
        <el-card shadow="never" v-loading="loading">
          <el-table :data="reviewList" stripe>
            <el-table-column label="成员" width="120">
              <template #default="{ row }">{{ memberName(row.authorId) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="80">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="periodKey" label="周期" width="150" />
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ row.submittedAt ? fmt(row.submittedAt) : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }"><el-button link type="primary" @click="openReviewItem(row)">评阅</el-button></template>
            </el-table-column>
            <template #empty><el-empty description="暂无待我评审的简报" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 成员简报 -->
      <el-tab-pane label="成员简报" name="members" lazy>
        <div class="bf__bar">
          <div class="bf__bar-right">
            <el-select v-model="memberFilter.authorId" placeholder="全部成员" clearable class="bf__quick" @change="loadMembersTab">
              <el-option v-for="m in revieweeMembers" :key="m.id" :label="m.name || m.username" :value="m.id" />
            </el-select>
            <el-select v-model="memberFilter.type" placeholder="全部类型" clearable class="bf__quick" @change="loadMembersTab">
              <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </div>
        </div>
        <el-card shadow="never" v-loading="loading">
          <el-table :data="memberList" stripe>
            <el-table-column label="成员" width="120">
              <template #default="{ row }">{{ memberName(row.authorId) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="80">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="periodKey" label="周期" width="150" />
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ row.submittedAt ? fmt(row.submittedAt) : '—' }}</template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }"><el-button link type="primary" @click="openReviewItem(row)">评阅</el-button></template>
            </el-table-column>
            <template #empty><el-empty description="暂无成员简报" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 跟进的问题 -->
      <el-tab-pane label="跟进的问题" name="issues" lazy>
        <el-card shadow="never" v-loading="loading">
          <el-table :data="issueList" stripe>
            <el-table-column prop="content" label="问题" min-width="200" />
            <el-table-column label="负责人" width="120">
              <template #default="{ row }">{{ memberName(row.ownerId) }}</template>
            </el-table-column>
            <el-table-column label="截止" width="120">
              <template #default="{ row }">{{ row.dueDate || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-select :model-value="row.status" size="small" @change="(s) => changeIssueStatus(row, s)">
                  <el-option v-for="(label, val) in ISSUE_STATUS" :key="val" :label="label" :value="val" />
                </el-select>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无跟进的问题" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 简报统计 -->
      <el-tab-pane label="简报统计" name="stats" lazy>
        <div class="bf__bar">
          <el-radio-group v-model="statsType" @change="loadStats">
            <el-radio-button value="daily">日报</el-radio-button>
            <el-radio-button value="weekly">周报</el-radio-button>
            <el-radio-button value="monthly">月报</el-radio-button>
          </el-radio-group>
        </div>
        <el-card shadow="never" v-loading="loading">
          <div v-if="stats" class="bf__stats-total">已提交合计：<b>{{ stats.total }}</b> 份</div>
          <el-table v-if="stats" :data="stats.members" stripe class="bf__stats-tb">
            <el-table-column label="成员">
              <template #default="{ row }">{{ memberName(row.authorId) }}</template>
            </el-table-column>
            <el-table-column prop="submittedCount" label="已提交份数" width="160" />
          </el-table>
          <el-empty v-if="stats && !stats.members.length" description="暂无提交" :image-size="60" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 填报/查看 抽屉 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="var(--mido-drawer-width)">
      <template v-if="currentTemplate">
        <div class="mido-briefing__period">
          周期：{{ form.periodKey }}
          <StatusTag v-if="form.status === 'submitted'" status="submitted" />
          <el-button v-if="!readOnly && !reviewMode" link type="primary" :loading="drafting" @click="genDraft">一键生成草稿</el-button>
        </div>
        <el-form label-position="top">
          <el-form-item v-for="f in currentTemplate.fields" :key="f.key" :label="f.label">
            <el-input v-model="form.content[f.key]" type="textarea" :rows="3" :disabled="readOnly"
              :placeholder="readOnly ? '' : '请输入' + f.label" />
          </el-form-item>
        </el-form>

        <!-- 评审批注 -->
        <template v-if="reviews.length || reviewMode">
          <el-divider>评审批注</el-divider>
          <div v-for="r in reviews" :key="r.id" class="mido-review">
            <div class="mido-review__meta">
              {{ memberName(r.reviewerId) }} · {{ fmt(r.reviewedAt) }}
              <el-tag v-if="r.action === 'approve'" size="small" type="success">已阅</el-tag>
            </div>
            <div>{{ r.comment }}</div>
          </div>
          <el-input v-if="reviewMode" v-model="reviewComment" type="textarea" :rows="2" placeholder="填写批注" class="mido-review__input" />
        </template>

        <!-- 提出跟进问题（已存在的简报可提） -->
        <template v-if="form.id">
          <el-divider>提出跟进问题</el-divider>
          <el-input v-model="issueContent" type="textarea" :rows="2" placeholder="待跟进的问题" />
          <div class="mido-issue__raise">
            <el-select v-model="issueOwner" placeholder="负责人(默认自己)" clearable class="bf__quick">
              <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
            </el-select>
            <el-button @click="raiseIssue">提出问题</el-button>
          </div>
        </template>
      </template>
      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button v-if="reviewMode" type="primary" :loading="saving" @click="submitReview">提交批注</el-button>
        <template v-else-if="!readOnly">
          <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="save(true)">提交</el-button>
        </template>
      </template>
    </el-drawer>

    <!-- 添加自定义模板 -->
    <el-dialog v-model="templateDialogVisible" title="添加模板" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="tplForm.name" placeholder="模板名称" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="tplForm.type" style="width: 100%">
            <el-option label="日报" value="daily" />
            <el-option label="周报" value="weekly" />
            <el-option label="月报" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段">
          <div v-for="(f, i) in tplForm.fields" :key="i" class="mido-tpl__field">
            <el-input v-model="f.label" placeholder="字段名，如：今日完成" />
            <el-button link type="danger" @click="removeTplField(i)">删除</el-button>
          </div>
          <el-button link type="primary" @click="addTplField">+ 添加字段</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 指派成员 -->
    <el-dialog v-model="assignDialogVisible" title="指派成员" width="480px">
      <el-select v-model="assignUserIds" multiple filterable placeholder="选择成员" style="width: 100%">
        <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import ViewSwitcher from '@/components/ViewSwitcher.vue'
import EmptyState from '@/components/EmptyState.vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled, Search, ArrowDown } from '@element-plus/icons-vue'
import { briefingApi } from '@/api/briefing'
import { fetchMembers } from '@/api/org'
import StatusTag from '@/components/StatusTag.vue'

const ISSUE_STATUS = { open: '待处理', following: '跟进中', closed: '已关闭' }
const TABS = ['all', 'submit', 'review', 'members', 'issues', 'stats']
const TYPE_OPTIONS = [
  { value: 'daily', label: '日报' },
  { value: 'weekly', label: '周报' },
  { value: 'monthly', label: '月报' },
]
const STATUS_OPTIONS = [
  { value: 'draft', label: '草稿' },
  { value: 'submitted', label: '已提交' },
]
const VIEWS = [
  { value: 'list', label: '列表' },
  { value: 'card', label: '卡片' },
]

const route = useRoute()
const activeTab = ref('all')
const loading = ref(false)
const templates = ref([])
const members = ref([])
const reviewees = ref([])
const reviewList = ref([])
const memberList = ref([])
const issueList = ref([])
const stats = ref(null)
const statsType = ref('daily')
const memberFilter = reactive({ authorId: null, type: null })

const revieweeMembers = computed(() => members.value.filter((m) => reviewees.value.includes(m.id)))

// 全部（我的简报）：一次拉取全类型，类型/状态/关键字客户端筛选 + 视图/分页/排序
const mineRows = ref([])
const view = ref('list')
const typeFilter = ref('')
const statusFilter = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const sortProp = ref('periodKey')
const sortOrder = ref('descending')
const filteredMine = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return mineRows.value.filter((r) => {
    if (typeFilter.value && r.type !== typeFilter.value) return false
    if (statusFilter.value && r.status !== statusFilter.value) return false
    if (kw && !`${r.periodKey || ''}`.toLowerCase().includes(kw)) return false
    return true
  })
})
const total = computed(() => filteredMine.value.length)
const sortedMine = computed(() => {
  const arr = [...filteredMine.value]
  if (!sortProp.value) return arr
  const dir = sortOrder.value === 'ascending' ? 1 : -1
  return arr.sort((a, b) => compareBy(a, b, sortProp.value) * dir)
})
const pagedMine = computed(() => sortedMine.value.slice((page.value - 1) * size.value, page.value * size.value))
function compareBy(a, b, prop) {
  const va = a[prop]
  const vb = b[prop]
  if (va == null && vb == null) return 0
  if (va == null) return -1
  if (vb == null) return 1
  return String(va).localeCompare(String(vb)) // periodKey/submittedAt 字典序即可
}
function onSortChange({ prop, order }) {
  sortProp.value = order ? prop : 'periodKey'
  sortOrder.value = order || 'descending'
  page.value = 1
}
watch([typeFilter, statusFilter, keyword], () => { page.value = 1 })

function typeShort(type) {
  return { daily: '日', weekly: '周', monthly: '月' }[type] || '报'
}
function typeLabel(type) {
  return { daily: '日报', weekly: '周报', monthly: '月报' }[type] || type
}
function memberName(id) {
  const m = members.value.find((x) => x.id === id)
  return m ? m.name || m.username : '—'
}
function fmt(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : ''
}

// ===== 各 tab 数据加载 =====
async function loadAllMine() {
  loading.value = true
  try {
    mineRows.value = await briefingApi.listMine(null) || []
  } finally {
    loading.value = false
  }
}
async function loadReview() {
  loading.value = true
  try {
    reviewList.value = await briefingApi.review(null) || []
  } finally {
    loading.value = false
  }
}
async function loadMembersTab() {
  loading.value = true
  try {
    if (!reviewees.value.length) reviewees.value = await briefingApi.reviewees() || []
    memberList.value = await briefingApi.members(memberFilter.type, memberFilter.authorId) || []
  } finally {
    loading.value = false
  }
}
async function loadIssues() {
  loading.value = true
  try {
    issueList.value = await briefingApi.listIssues(null) || []
  } finally {
    loading.value = false
  }
}
async function loadStats() {
  loading.value = true
  try {
    stats.value = await briefingApi.stats(statsType.value)
  } finally {
    loading.value = false
  }
}
async function changeIssueStatus(row, status) {
  await briefingApi.updateIssueStatus(row.id, status)
  ElMessage.success('已更新')
  await loadIssues()
}

// tab → 数据加载（顶部导航 router.push(?tab=) 驱动 route.query.tab → activeTab）
function onTab(t) {
  if (t === 'all') loadAllMine()
  else if (t === 'review') loadReview()
  else if (t === 'members') loadMembersTab()
  else if (t === 'issues') loadIssues()
  else if (t === 'stats') loadStats()
}
const normalizeTab = (t) => (TABS.includes(t) ? t : 'all')
watch(() => route.query.tab, (t) => {
  const nt = normalizeTab(t)
  activeTab.value = nt
  onTab(nt)
}, { immediate: true })

// ===== 填报 =====
const drawerVisible = ref(false)
const saving = ref(false)
const currentTemplate = ref(null)
const form = reactive({ id: null, periodKey: '', periodStart: '', periodEnd: '', content: {}, status: 'draft' })
const reviewMode = ref(false)
const reviews = ref([])
const reviewComment = ref('')
const readOnly = computed(() => form.status === 'submitted' || reviewMode.value)
const drawerTitle = computed(() => (currentTemplate.value ? currentTemplate.value.name : '简报'))

function periodOf(type) {
  const now = dayjs()
  if (type === 'weekly') {
    const mon = now.day(1)
    return { key: mon.format('YYYY-MM-DD'), start: mon.format('YYYY-MM-DD'), end: mon.add(6, 'day').format('YYYY-MM-DD') }
  }
  if (type === 'monthly') {
    const s = now.startOf('month')
    return { key: s.format('YYYY-MM'), start: s.format('YYYY-MM-DD'), end: now.endOf('month').format('YYYY-MM-DD') }
  }
  return { key: now.format('YYYY-MM-DD'), start: now.format('YYYY-MM-DD'), end: now.format('YYYY-MM-DD') }
}

function openNew(template) {
  currentTemplate.value = template
  const p = periodOf(template.type)
  form.id = null
  form.periodKey = p.key
  form.periodStart = p.start
  form.periodEnd = p.end
  form.content = {}
  form.status = 'draft'
  reviewMode.value = false
  reviews.value = []
  drawerVisible.value = true
}
function openNewById(id) {
  const tpl = templates.value.find((t) => t.id === id)
  if (tpl) openNew(tpl)
}

async function loadBriefingIntoForm(row, asReviewer) {
  const tpl = templates.value.find((t) => t.id === row.templateId)
  if (!tpl) {
    ElMessage.warning('模板不存在')
    return false
  }
  const full = await briefingApi.get(row.id)
  currentTemplate.value = tpl
  form.id = full.id
  form.periodKey = full.periodKey
  form.periodStart = full.periodStart
  form.periodEnd = full.periodEnd
  form.content = { ...(full.content || {}) }
  form.status = full.status
  reviewMode.value = asReviewer
  reviewComment.value = ''
  issueContent.value = ''
  issueOwner.value = null
  reviews.value = await briefingApi.reviews(row.id)
  drawerVisible.value = true
  return true
}
function openExisting(row) {
  loadBriefingIntoForm(row, false)
}
function openReviewItem(row) {
  loadBriefingIntoForm(row, true)
}

async function submitReview() {
  if (!reviewComment.value.trim()) {
    ElMessage.warning('请填写批注')
    return
  }
  saving.value = true
  try {
    await briefingApi.addReview(form.id, { comment: reviewComment.value, action: 'comment' })
    ElMessage.success('批注已提交')
    reviewComment.value = ''
    reviews.value = await briefingApi.reviews(form.id)
  } finally {
    saving.value = false
  }
}

// 一键生成草稿（规则式）
const drafting = ref(false)
async function genDraft() {
  drafting.value = true
  try {
    const content = await briefingApi.draft(currentTemplate.value.id, form.periodStart, form.periodEnd)
    form.content = { ...form.content, ...content }
    ElMessage.success('已生成草稿，可继续编辑')
  } finally {
    drafting.value = false
  }
}

// 提出跟进问题
const issueContent = ref('')
const issueOwner = ref(null)
async function raiseIssue() {
  if (!issueContent.value.trim()) {
    ElMessage.warning('请填写问题内容')
    return
  }
  await briefingApi.createIssue({
    briefingId: form.id,
    content: issueContent.value,
    ownerId: issueOwner.value || undefined,
  })
  ElMessage.success('问题已提出')
  issueContent.value = ''
  issueOwner.value = null
}

async function save(submit) {
  saving.value = true
  try {
    const id = await briefingApi.save({
      templateId: currentTemplate.value.id,
      periodKey: form.periodKey,
      periodStart: form.periodStart,
      periodEnd: form.periodEnd,
      content: form.content,
    })
    if (submit) {
      await briefingApi.submit(id)
      ElMessage.success('已提交')
    } else {
      ElMessage.success('已保存草稿')
    }
    drawerVisible.value = false
    if (activeTab.value === 'all') await loadAllMine()
  } finally {
    saving.value = false
  }
}

// ===== 自定义模板 / 指派 =====
const templateDialogVisible = ref(false)
const tplForm = reactive({ name: '', type: 'daily', fields: [{ label: '' }] })
function openTemplateDialog() {
  tplForm.name = ''
  tplForm.type = 'daily'
  tplForm.fields = [{ label: '' }]
  templateDialogVisible.value = true
}
function addTplField() {
  tplForm.fields.push({ label: '' })
}
function removeTplField(i) {
  tplForm.fields.splice(i, 1)
}
async function saveTemplate() {
  const fields = tplForm.fields
    .filter((f) => f.label.trim())
    .map((f, i) => ({ key: 'f' + (i + 1), label: f.label.trim(), type: 'textarea' }))
  if (!tplForm.name.trim() || !fields.length) {
    ElMessage.warning('请填写模板名称与至少一个字段')
    return
  }
  await briefingApi.createTemplate({ name: tplForm.name, type: tplForm.type, fields })
  ElMessage.success('模板已创建')
  templateDialogVisible.value = false
  templates.value = await briefingApi.templates()
}

const assignDialogVisible = ref(false)
const assignTpl = ref(null)
const assignUserIds = ref([])
async function onTemplateCmd(cmd, t) {
  if (cmd === 'disable') {
    await ElMessageBox.confirm(`停用模板「${t.name}」？`, '提示', { type: 'warning' })
    await briefingApi.disableTemplate(t.id)
    ElMessage.success('已停用')
    templates.value = await briefingApi.templates()
  } else if (cmd === 'assign') {
    assignTpl.value = t
    assignUserIds.value = []
    assignDialogVisible.value = true
  }
}
async function saveAssign() {
  await briefingApi.assignTemplate(assignTpl.value.id, { userIds: assignUserIds.value, deptIds: [] })
  ElMessage.success('指派已保存')
  assignDialogVisible.value = false
}

onMounted(async () => {
  ;[templates.value, members.value] = await Promise.all([briefingApi.templates(), fetchMembers()])
})
</script>

<style scoped>
/* 顶部导航(WorkspaceShell)已提供 tab 切换，隐藏 el-tabs 自带头避免双层 */
.bf__tabs--headless :deep(.el-tabs__header) {
  display: none;
}
.bf__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-3);
  margin-bottom: var(--mido-space-4);
}
.bf__bar-right {
  display: flex;
  gap: var(--mido-space-2);
}
.bf__quick {
  width: calc(var(--mido-nav-width) * 0.7);
}
.bf__search {
  width: var(--mido-nav-width);
}
.bf__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-4);
}
.bf__stats-total {
  margin-bottom: var(--mido-space-3);
}
/* 卡片视图：自适应网格 */
.bf__cards {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-4);
}
/* 卡片视图空态：占满整行并居中，避免被 flex 挤在左侧小框（与列表视图一致） */
.bf__cards-empty {
  width: 100%;
}
.bf__card {
  width: calc(var(--mido-nav-width) * 1.1);
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.bf__card:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}
.bf__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bf__card-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--mido-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-white);
  font-weight: var(--mido-font-weight-bold);
}
.bf__card-icon.is-daily {
  background: var(--mido-brief-daily);
}
.bf__card-icon.is-weekly {
  background: var(--mido-brief-weekly);
}
.bf__card-icon.is-monthly {
  background: var(--mido-brief-monthly);
}
.bf__card-period {
  font-weight: var(--mido-font-weight-bold);
}
.bf__card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
/* 模板卡（提交简报） */
.mido-card {
  width: calc(var(--mido-nav-width) * 1.1);
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
}
.mido-card:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}
.mido-card__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--mido-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-white);
  font-weight: var(--mido-font-weight-bold);
}
.mido-card__icon.is-daily {
  background: var(--mido-brief-daily);
}
.mido-card__icon.is-weekly {
  background: var(--mido-brief-weekly);
}
.mido-card__icon.is-monthly {
  background: var(--mido-brief-monthly);
}
.mido-card__name {
  flex: 1;
}
.mido-card__more {
  color: var(--el-text-color-secondary);
}
.mido-tpl__field {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
  width: 100%;
}
.mido-briefing__period {
  margin-bottom: var(--mido-space-3);
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.mido-review {
  padding: var(--mido-space-2) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-lighter);
}
.mido-review__meta {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-secondary);
  margin-bottom: var(--mido-space-1);
}
.mido-review__input {
  margin-top: var(--mido-space-3);
}
.mido-issue__raise {
  display: flex;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-2);
}
</style>
