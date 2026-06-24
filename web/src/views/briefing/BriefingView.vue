<template>
  <div class="mido-briefing">
    <!-- 左栏 -->
    <div class="mido-briefing__side">
      <div class="mido-briefing__title">简报</div>
      <div
        v-for="m in menus"
        :key="m.key"
        class="mido-briefing__menu"
        :class="{ 'is-active': active === m.key }"
        @click="selectMenu(m.key)"
      >
        {{ m.label }}
      </div>
    </div>

    <!-- 主区 -->
    <div class="mido-briefing__main" v-loading="loading">
      <!-- 提交简报：模板卡片 -->
      <template v-if="active === 'submit'">
        <div class="mido-briefing__headrow">
          <div class="mido-briefing__head">提交简报</div>
          <el-button type="primary" :icon="Plus" @click="openTemplateDialog">添加模板</el-button>
        </div>
        <div class="mido-briefing__cards">
          <div
            v-for="t in templates"
            :key="t.id"
            class="mido-card"
            @click="openNew(t)"
          >
            <div class="mido-card__icon" :class="'is-' + t.type">{{ typeShort(t.type) }}</div>
            <div class="mido-card__name">{{ t.name }}</div>
            <el-dropdown
              v-if="!t.isBuiltin"
              trigger="click"
              @click.stop
              @command="(cmd) => onTemplateCmd(cmd, t)"
            >
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
      </template>

      <!-- 我的日/周/月报：列表 -->
      <template v-else-if="isMine">
        <div class="mido-briefing__head">{{ menuLabel }}</div>
        <el-table :data="myList" style="width: 100%">
          <el-table-column prop="periodKey" label="周期" width="160" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="180">
            <template #default="{ row }">{{ row.submittedAt ? fmt(row.submittedAt) : '—' }}</template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button link type="primary" @click="openExisting(row)">
                {{ row.status === 'submitted' ? '查看' : '编辑' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!myList.length" :description="'暂无' + menuLabel" />
      </template>

      <!-- 简报统计 -->
      <template v-else-if="active === 'stats'">
        <div class="mido-briefing__head">简报统计</div>
        <el-radio-group v-model="statsType" @change="loadStats" style="margin-bottom: 16px">
          <el-radio-button value="daily">日报</el-radio-button>
          <el-radio-button value="weekly">周报</el-radio-button>
          <el-radio-button value="monthly">月报</el-radio-button>
        </el-radio-group>
        <div v-if="stats" class="mido-stats__total">已提交合计：<b>{{ stats.total }}</b> 份</div>
        <el-table v-if="stats" :data="stats.members" style="width: 100%; margin-top: 12px">
          <el-table-column label="成员">
            <template #default="{ row }">{{ memberName(row.authorId) }}</template>
          </el-table-column>
          <el-table-column prop="submittedCount" label="已提交份数" width="160" />
        </el-table>
        <el-empty v-if="stats && !stats.members.length" description="暂无提交" />
      </template>

      <!-- 跟进的问题 -->
      <template v-else-if="active === 'issues'">
        <div class="mido-briefing__head">跟进的问题</div>
        <el-table :data="issueList" style="width: 100%">
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
        </el-table>
        <el-empty v-if="!issueList.length" description="暂无跟进的问题" />
      </template>

      <!-- 我评审的 / 成员简报 -->
      <template v-else>
        <div class="mido-briefing__head">{{ menuLabel }}</div>
        <div v-if="active === 'members'" class="mido-briefing__filter">
          <el-select v-model="memberFilter.authorId" placeholder="选择成员" clearable @change="loadReviewList">
            <el-option v-for="m in revieweeMembers" :key="m.id" :label="m.name || m.username" :value="m.id" />
          </el-select>
          <el-select v-model="memberFilter.type" placeholder="类型" clearable @change="loadReviewList">
            <el-option label="日报" value="daily" />
            <el-option label="周报" value="weekly" />
            <el-option label="月报" value="monthly" />
          </el-select>
        </div>
        <el-table :data="reviewList" style="width: 100%">
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
            <template #default="{ row }">
              <el-button link type="primary" @click="openReviewItem(row)">评阅</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!reviewList.length" :description="'暂无' + menuLabel" />
      </template>
    </div>

    <!-- 填报/查看 抽屉 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="var(--mido-drawer-width)">
      <template v-if="currentTemplate">
        <div class="mido-briefing__period">
          周期：{{ form.periodKey }}
          <StatusTag v-if="form.status === 'submitted'" status="submitted" />
          <el-button
            v-if="!readOnly && !reviewMode"
            link
            type="primary"
            :loading="drafting"
            @click="genDraft"
          >一键生成草稿</el-button>
        </div>
        <el-form label-position="top">
          <el-form-item v-for="f in currentTemplate.fields" :key="f.key" :label="f.label">
            <el-input
              v-model="form.content[f.key]"
              type="textarea"
              :rows="3"
              :disabled="readOnly"
              :placeholder="readOnly ? '' : '请输入' + f.label"
            />
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
          <el-input
            v-if="reviewMode"
            v-model="reviewComment"
            type="textarea"
            :rows="2"
            placeholder="填写批注"
            class="mido-review__input"
          />
        </template>

        <!-- 提出跟进问题（已存在的简报可提） -->
        <template v-if="form.id">
          <el-divider>提出跟进问题</el-divider>
          <el-input v-model="issueContent" type="textarea" :rows="2" placeholder="待跟进的问题" />
          <div class="mido-issue__raise">
            <el-select v-model="issueOwner" placeholder="负责人(默认自己)" clearable style="width: 200px">
              <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
            </el-select>
            <el-button @click="raiseIssue">提出问题</el-button>
          </div>
        </template>
      </template>
      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button
          v-if="reviewMode"
          type="primary"
          :loading="saving"
          @click="submitReview"
        >提交批注</el-button>
        <template v-else-if="!readOnly">
          <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="save(true)">提交</el-button>
        </template>
      </template>
    </el-drawer>

    <!-- 添加自定义模板 -->
    <el-dialog v-model="templateDialogVisible" title="添加模板" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="tplForm.name" placeholder="模板名称" />
        </el-form-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled } from '@element-plus/icons-vue'
import { briefingApi } from '@/api/briefing'
import { fetchMembers } from '@/api/org'
import StatusTag from '@/components/StatusTag.vue'

const menus = [
  { key: 'submit', label: '提交简报' },
  { key: 'daily', label: '我的日报' },
  { key: 'weekly', label: '我的周报' },
  { key: 'monthly', label: '我的月报' },
  { key: 'review', label: '我评审的' },
  { key: 'members', label: '成员简报' },
  { key: 'issues', label: '跟进的问题' },
  { key: 'stats', label: '简报统计' },
]
const ISSUE_STATUS = { open: '待处理', following: '跟进中', closed: '已关闭' }
const MINE = ['daily', 'weekly', 'monthly']

const active = ref('submit')
const loading = ref(false)
const templates = ref([])
const myList = ref([])
const members = ref([])
const reviewees = ref([])
const reviewList = ref([])
const issueList = ref([])
const stats = ref(null)
const statsType = ref('daily')
const memberFilter = reactive({ authorId: null, type: null })

const menuLabel = computed(() => menus.find((m) => m.key === active.value)?.label || '')
const isMine = computed(() => MINE.includes(active.value))
const revieweeMembers = computed(() => members.value.filter((m) => reviewees.value.includes(m.id)))

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

async function selectMenu(key) {
  active.value = key
  if (isMine.value) {
    await loadMine(key)
  } else if (key === 'review') {
    await loadReviewList()
  } else if (key === 'members') {
    reviewees.value = await briefingApi.reviewees()
    memberFilter.authorId = null
    memberFilter.type = null
    await loadReviewList()
  } else if (key === 'issues') {
    await loadIssues()
  } else if (key === 'stats') {
    statsType.value = 'daily'
    await loadStats()
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

async function loadIssues() {
  loading.value = true
  try {
    issueList.value = await briefingApi.listIssues(null)
  } finally {
    loading.value = false
  }
}

async function changeIssueStatus(row, status) {
  await briefingApi.updateIssueStatus(row.id, status)
  ElMessage.success('已更新')
  await loadIssues()
}

async function loadReviewList() {
  loading.value = true
  try {
    reviewList.value = active.value === 'members'
      ? await briefingApi.members(memberFilter.type, memberFilter.authorId)
      : await briefingApi.review(null)
  } finally {
    loading.value = false
  }
}

async function loadMine(type) {
  loading.value = true
  try {
    myList.value = await briefingApi.listMine(type)
  } finally {
    loading.value = false
  }
}

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
    if (active.value !== 'submit') {
      await loadMine(active.value)
    }
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
.mido-briefing {
  display: flex;
  height: 100%;
  /* 与全站一致的「白卡浮于灰底」外框（对齐 el-card 视觉）：此前缺外框，整块边到边显得扁平、与其他页对比突兀 */
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  overflow: hidden;
}
.mido-briefing__side {
  width: 200px;
  border-right: 1px solid var(--el-border-color-lighter);
  padding: 16px 8px;
  flex-shrink: 0;
}
.mido-briefing__title {
  font-size: 16px;
  font-weight: 600;
  padding: 0 12px 12px;
}
.mido-briefing__menu {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}
.mido-briefing__menu:hover {
  background: var(--el-fill-color-light);
}
.mido-briefing__menu.is-active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
.mido-briefing__main {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
}
.mido-briefing__head {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}
.mido-briefing__cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.mido-card {
  width: 220px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
}
.mido-card:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}
.mido-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-white);
  font-weight: 600;
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
  font-size: 15px;
  flex: 1;
}
.mido-card__more {
  color: var(--el-text-color-secondary);
}
.mido-briefing__headrow {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mido-tpl__field {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  width: 100%;
}
.mido-briefing__period {
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
}
.mido-briefing__filter {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.mido-review {
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.mido-review__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.mido-review__input {
  margin-top: 12px;
}
.mido-issue__raise {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
