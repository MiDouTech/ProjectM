<template>
  <div class="mido-calendar">
    <WorkspaceShell module="calendar" />
    <!-- 顶部工具条 -->
    <div class="mido-calendar__bar">
      <div class="mido-calendar__nav">
        <el-radio-group v-model="viewMode">
          <el-radio-button value="month">月</el-radio-button>
          <el-radio-button value="week">周</el-radio-button>
          <el-radio-button value="day">日</el-radio-button>
        </el-radio-group>
        <el-button-group>
          <el-button @click="shift(-1)">上一{{ unitLabel }}</el-button>
          <el-button @click="goToday">今天</el-button>
          <el-button @click="shift(1)">下一{{ unitLabel }}</el-button>
        </el-button-group>
        <span class="mido-calendar__period">{{ periodLabel }}</span>
      </div>
      <el-select
        v-model="busyUserIds"
        multiple
        collapse-tags
        clearable
        placeholder="成员忙闲"
        style="width: 180px"
      >
        <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
      </el-select>
      <el-button :icon="Connection" @click="subscribeCalendar">订阅</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate()">新建日程</el-button>
    </div>

    <div v-loading="loading" class="mido-calendar__body">
      <!-- 月视图 -->
      <div v-if="viewMode === 'month'" class="mido-month">
        <div class="mido-month__head">
          <span v-for="w in weekHeads" :key="w">周{{ w }}</span>
        </div>
        <div class="mido-month__grid">
          <div
            v-for="cell in monthCells"
            :key="cell.key"
            class="mido-month__cell"
            :class="{ 'is-other': !cell.inMonth, 'is-today': cell.isToday }"
            @click="openCreate(cell.date)"
          >
            <div class="mido-month__date">{{ cell.date.date() }}</div>
            <div class="mido-month__events">
              <div
                v-for="s in eventsOf(cell.date)"
                :key="s.id + '-' + (s.occurrenceDate || 's')"
                class="mido-event"
                @click.stop="openDetail(s)"
              >
                <span class="mido-event__dot"></span>
                <span class="mido-event__text">{{ timeShort(s) }} {{ s.title }}</span>
              </div>
              <div
                v-for="t in tasksOf(cell.date)"
                :key="'t' + t.id"
                class="mido-event mido-event--task"
                @click.stop="openTask(t)"
              >
                <span class="mido-event__dot mido-event__dot--task"></span>
                <span class="mido-event__text">{{ t.isMilestone ? '📌' : '✔' }} {{ t.title }}</span>
              </div>
              <div
                v-for="(b, i) in busyOf(cell.date)"
                :key="'b' + i"
                class="mido-event mido-event--busy"
              >
                <span class="mido-event__dot mido-event__dot--busy"></span>
                <span class="mido-event__text">忙·{{ memberName(b.userId) }} {{ busyTime(b) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 周视图 -->
      <div v-else-if="viewMode === 'week'" class="mido-list">
        <div v-for="day in weekDays" :key="day.format('YYYY-MM-DD')" class="mido-list__day">
          <div class="mido-list__date" :class="{ 'is-today': isToday(day) }">
            {{ day.format('MM-DD') }} 周{{ weekHeads[day.day()] }}
          </div>
          <div v-if="eventsOf(day).length || tasksOf(day).length || busyOf(day).length" class="mido-list__events">
            <div
              v-for="s in eventsOf(day)"
              :key="s.id + '-' + (s.occurrenceDate || 's')"
              class="mido-event"
              @click="openDetail(s)"
            >
              <span class="mido-event__dot"></span>
              <span class="mido-event__text">{{ timeShort(s) }} {{ s.title }}</span>
            </div>
            <div
              v-for="t in tasksOf(day)"
              :key="'t' + t.id"
              class="mido-event mido-event--task"
              @click="openTask(t)"
            >
              <span class="mido-event__dot mido-event__dot--task"></span>
              <span class="mido-event__text">{{ t.isMilestone ? '📌' : '✔' }} {{ t.title }}</span>
            </div>
            <div v-for="(b, i) in busyOf(day)" :key="'b' + i" class="mido-event mido-event--busy">
              <span class="mido-event__dot mido-event__dot--busy"></span>
              <span class="mido-event__text">忙·{{ memberName(b.userId) }} {{ busyTime(b) }}</span>
            </div>
          </div>
          <div v-else class="mido-list__empty">无日程</div>
        </div>
      </div>

      <!-- 日视图 -->
      <div v-else class="mido-list">
        <div class="mido-list__day">
          <div class="mido-list__date" :class="{ 'is-today': isToday(anchor) }">
            {{ anchor.format('YYYY-MM-DD') }} 周{{ weekHeads[anchor.day()] }}
          </div>
          <div v-if="eventsOf(anchor).length || tasksOf(anchor).length || busyOf(anchor).length" class="mido-list__events">
            <div
              v-for="s in eventsOf(anchor)"
              :key="s.id + '-' + (s.occurrenceDate || 's')"
              class="mido-event"
              @click="openDetail(s)"
            >
              <span class="mido-event__dot"></span>
              <span class="mido-event__text">{{ timeShort(s) }} {{ s.title }}</span>
            </div>
            <div
              v-for="t in tasksOf(anchor)"
              :key="'t' + t.id"
              class="mido-event mido-event--task"
              @click="openTask(t)"
            >
              <span class="mido-event__dot mido-event__dot--task"></span>
              <span class="mido-event__text">{{ t.isMilestone ? '📌' : '✔' }} {{ t.title }}</span>
            </div>
            <div v-for="(b, i) in busyOf(anchor)" :key="'b' + i" class="mido-event mido-event--busy">
              <span class="mido-event__dot mido-event__dot--busy"></span>
              <span class="mido-event__text">忙·{{ memberName(b.userId) }} {{ busyTime(b) }}</span>
            </div>
          </div>
          <el-empty v-else description="今日无日程" />
        </div>
      </div>
    </div>

    <!-- 新建/编辑 抽屉 -->
    <el-drawer v-model="formVisible" :title="form.id ? '编辑日程' : '新建日程'" size="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入日程标题" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-checkbox v-model="form.allDay">全天</el-checkbox>
          <el-checkbox v-model="form.allowFeedback">允许参与人反馈(RSVP)</el-checkbox>
        </el-form-item>
        <el-form-item v-if="!form.id" label="日历">
          <el-select v-model="form.calendarId" placeholder="默认「我的日程」" style="width: 100%" clearable>
            <el-option v-for="c in calendars" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人">
          <el-select
            v-model="form.participantUserIds"
            multiple
            filterable
            placeholder="选择参与人"
            style="width: 100%"
          >
            <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="重复">
              <el-select v-model="form.recurrence" style="width: 100%">
                <el-option v-for="o in RECUR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提醒">
              <el-select v-model="form.reminderMinutes" multiple placeholder="可多选" style="width: 100%">
                <el-option v-for="o in REMINDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="资源（会议室/设备）">
          <el-select
            v-model="form.resourceIds"
            multiple
            placeholder="选择资源（冲突将被拦截）"
            style="width: 100%"
          >
            <el-option
              v-for="r in resources"
              :key="r.id"
              :label="r.name + (r.location ? '（' + r.location + '）' : '')"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" :underline="false" @click="openSlotAssistant">打开日程排期小助手</el-link>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="请输入地点" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-drawer>

    <!-- 详情 抽屉 -->
    <el-drawer v-model="detailVisible" title="日程详情" size="420px">
      <template v-if="detail">
        <h3 class="mido-detail__title">{{ detail.title }}</h3>
        <p class="mido-detail__time">
          {{ fmt(detail.startTime) }} ~ {{ fmt(detail.endTime) }}
          <el-tag v-if="detail.allDay" size="small" type="info">全天</el-tag>
          <el-tag v-if="detail.recurring" size="small" type="success">循环</el-tag>
        </p>
        <p v-if="detail.location">📍 {{ detail.location }}</p>
        <p v-if="detail.description" class="mido-detail__desc">{{ detail.description }}</p>
        <el-divider>参与人</el-divider>
        <div v-for="p in detail.participants" :key="p.id" class="mido-detail__pt">
          <span>{{ memberName(p.userId) || p.externalName || '—' }}</span>
          <el-tag size="small" :type="rsvpType(p.rsvpStatus)">{{ rsvpLabel(p.rsvpStatus) }}</el-tag>
          <el-tag v-if="p.role === 'organizer'" size="small" type="warning">组织者</el-tag>
        </div>
        <el-divider v-if="detail.allowFeedback">我的反馈</el-divider>
        <div v-if="detail.allowFeedback" class="mido-detail__rsvp">
          <el-button size="small" type="success" @click="doRsvp('accepted')">参加</el-button>
          <el-button size="small" @click="doRsvp('tentative')">暂定</el-button>
          <el-button size="small" type="danger" @click="doRsvp('declined')">谢绝</el-button>
        </div>
      </template>
      <template #footer>
        <el-button
          v-if="canEdit && detailOccurrenceDate"
          @click="cancelThisOccurrence"
        >取消该次</el-button>
        <el-button v-if="canEdit" type="primary" @click="openEdit">编辑</el-button>
        <el-button v-if="canEdit" type="danger" @click="remove">删除{{ detail && detail.recurring ? '整个循环' : '' }}</el-button>
      </template>
    </el-drawer>

    <!-- 排期小助手 -->
    <el-dialog v-model="slotDialogVisible" title="日程排期小助手" width="520px">
      <el-form :model="slotForm" label-width="80px">
        <el-form-item label="参选人">
          <el-select v-model="slotForm.userIds" multiple filterable style="width: 100%">
            <el-option v-for="m in members" :key="m.id" :label="m.name || m.username" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="slotForm.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时长">
          <el-select v-model="slotForm.durationMinutes" style="width: 100%">
            <el-option :value="30" label="30 分钟" />
            <el-option :value="60" label="1 小时" />
            <el-option :value="90" label="1.5 小时" />
            <el-option :value="120" label="2 小时" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="slotLoading" @click="searchSlots">查找空档</el-button>
      <div v-if="slotsSearched" class="mido-slots">
        <el-empty v-if="!slots.length" description="所选日期无共同空档" :image-size="60" />
        <div
          v-for="(s, i) in slots"
          :key="i"
          class="mido-slot"
          @click="pickSlot(s)"
        >
          {{ dayjs(s.start).format('HH:mm') }} ~ {{ dayjs(s.end).format('HH:mm') }}
          <el-tag size="small" type="success">可用</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import WorkspaceShell from '@/components/WorkspaceShell.vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Plus, Connection } from '@element-plus/icons-vue'
import { calendarApi } from '@/api/calendar'
import { fetchMembers } from '@/api/org'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const weekHeads = ['日', '一', '二', '三', '四', '五', '六']

// 循环/提醒可选项
const RECUR_OPTIONS = [
  { label: '不重复', value: '' },
  { label: '每天', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '每月', value: 'MONTHLY' },
  { label: '每年', value: 'YEARLY' },
]
const REMINDER_OPTIONS = [
  { label: '提前 5 分钟', value: 5 },
  { label: '提前 15 分钟', value: 15 },
  { label: '提前 30 分钟', value: 30 },
  { label: '提前 1 小时', value: 60 },
  { label: '提前 1 天', value: 1440 },
]
function buildRecurRule(freq) {
  return freq ? JSON.stringify({ freq, interval: 1 }) : null
}
function parseFreq(recurRule) {
  if (!recurRule) return ''
  try {
    return JSON.parse(recurRule).freq || ''
  } catch {
    return ''
  }
}

const viewMode = ref('month')
const anchor = ref(dayjs())
const loading = ref(false)
const schedules = ref([])
const calendars = ref([])
const members = ref([])
const resources = ref([])
const tasks = ref([])
const busyUserIds = ref([])
const busyBlocks = ref([])

const unitLabel = computed(() => ({ month: '月', week: '周', day: '日' }[viewMode.value]))
const periodLabel = computed(() => {
  if (viewMode.value === 'month') return anchor.value.format('YYYY年MM月')
  if (viewMode.value === 'week') {
    const s = anchor.value.startOf('week')
    return `${s.format('YYYY-MM-DD')} ~ ${s.add(6, 'day').format('MM-DD')}`
  }
  return anchor.value.format('YYYY年MM月DD日')
})

// 可见区间 [from, to]
const visibleRange = computed(() => {
  let from
  let to
  if (viewMode.value === 'month') {
    from = anchor.value.startOf('month').startOf('week')
    to = anchor.value.endOf('month').endOf('week')
  } else if (viewMode.value === 'week') {
    from = anchor.value.startOf('week')
    to = anchor.value.endOf('week')
  } else {
    from = anchor.value.startOf('day')
    to = anchor.value.endOf('day')
  }
  return { from, to }
})

const monthCells = computed(() => {
  const { from, to } = visibleRange.value
  const cells = []
  let d = from
  while (d.isBefore(to) || d.isSame(to, 'day')) {
    cells.push({
      key: d.format('YYYY-MM-DD'),
      date: d,
      inMonth: d.month() === anchor.value.month(),
      isToday: d.isSame(dayjs(), 'day'),
    })
    d = d.add(1, 'day')
  }
  return cells
})

const weekDays = computed(() => {
  const s = anchor.value.startOf('week')
  return Array.from({ length: 7 }, (_, i) => s.add(i, 'day'))
})

function eventsOf(date) {
  const key = date.format('YYYY-MM-DD')
  return schedules.value.filter((s) => {
    const sd = dayjs(s.startTime).format('YYYY-MM-DD')
    const ed = dayjs(s.endTime).format('YYYY-MM-DD')
    return key >= sd && key <= ed
  })
}

function tasksOf(date) {
  const key = date.format('YYYY-MM-DD')
  return tasks.value.filter((t) => t.dueDate === key)
}

function busyOf(date) {
  const key = date.format('YYYY-MM-DD')
  return busyBlocks.value.filter((b) => {
    const sd = dayjs(b.start).format('YYYY-MM-DD')
    const ed = dayjs(b.end).format('YYYY-MM-DD')
    return key >= sd && key <= ed
  })
}

function openTask(t) {
  router.push(`/project/${t.projectId}/task/${t.id}`)
}

function isToday(d) {
  return d.isSame(dayjs(), 'day')
}

function timeShort(s) {
  return s.allDay ? '全天' : dayjs(s.startTime).format('HH:mm')
}

function busyTime(b) {
  return b.allDay ? '全天' : `${dayjs(b.start).format('HH:mm')}-${dayjs(b.end).format('HH:mm')}`
}

function fmt(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : ''
}

function memberName(uid) {
  const m = members.value.find((x) => x.id === uid)
  return m ? m.name || m.username : ''
}

const RSVP_LABEL = { pending: '待反馈', accepted: '参加', tentative: '暂定', declined: '谢绝' }
const RSVP_TYPE = { pending: 'info', accepted: 'success', tentative: 'warning', declined: 'danger' }
const rsvpLabel = (s) => RSVP_LABEL[s] || s
const rsvpType = (s) => RSVP_TYPE[s] || 'info'

async function load() {
  loading.value = true
  try {
    const { from, to } = visibleRange.value
    const [sch, tk] = await Promise.all([
      calendarApi.range(from.format('YYYY-MM-DDTHH:mm:ss'), to.format('YYYY-MM-DDTHH:mm:ss')),
      calendarApi.tasksInRange(from.format('YYYY-MM-DD'), to.format('YYYY-MM-DD')),
    ])
    schedules.value = sch
    tasks.value = tk
    busyBlocks.value = busyUserIds.value.length
      ? await calendarApi.freeBusy(busyUserIds.value, from.format('YYYY-MM-DDTHH:mm:ss'), to.format('YYYY-MM-DDTHH:mm:ss'))
      : []
  } finally {
    loading.value = false
  }
}

async function subscribeCalendar() {
  const def = calendars.value.find((c) => c.isDefault) || calendars.value[0]
  if (!def) {
    ElMessage.warning('暂无可订阅日历')
    return
  }
  const { icsUrl } = await calendarApi.subscribe(def.id)
  const fullUrl = location.origin + icsUrl
  try {
    await navigator.clipboard.writeText(fullUrl)
    ElMessage.success('订阅地址已复制到剪贴板')
  } catch {
    // 剪贴板不可用时弹出地址供手动复制
  }
  ElMessageBox.alert(fullUrl, '日历订阅地址（ics）', { confirmButtonText: '知道了' })
}

function shift(n) {
  anchor.value = anchor.value.add(n, viewMode.value)
}
function goToday() {
  anchor.value = dayjs()
}

watch([viewMode, anchor, busyUserIds], load)

// ===== 表单 =====
const formVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  calendarId: null,
  title: '',
  startTime: '',
  endTime: '',
  allDay: false,
  allowFeedback: true,
  location: '',
  description: '',
  participantUserIds: [],
  recurrence: '',
  reminderMinutes: [],
  resourceIds: [],
})

function resetForm(date) {
  const base = date || dayjs()
  form.id = null
  form.calendarId = null
  form.title = ''
  form.startTime = base.hour(9).minute(0).second(0).format('YYYY-MM-DDTHH:mm:ss')
  form.endTime = base.hour(10).minute(0).second(0).format('YYYY-MM-DDTHH:mm:ss')
  form.allDay = false
  form.allowFeedback = true
  form.location = ''
  form.description = ''
  form.participantUserIds = []
  form.recurrence = ''
  form.reminderMinutes = []
  form.resourceIds = []
}

function openCreate(date) {
  resetForm(date)
  formVisible.value = true
}

async function submit() {
  if (!form.title || !form.startTime || !form.endTime) {
    ElMessage.warning('标题与起止时间必填')
    return
  }
  if (dayjs(form.endTime).isSame(form.startTime) || dayjs(form.endTime).isBefore(form.startTime)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  saving.value = true
  try {
    const participants = (form.participantUserIds || []).map((uid) => ({ userId: uid, role: 'required' }))
    const payload = {
      title: form.title,
      description: form.description,
      startTime: form.startTime,
      endTime: form.endTime,
      allDay: form.allDay,
      location: form.location,
      allowFeedback: form.allowFeedback,
      participants,
      resourceIds: form.resourceIds,
      recurRule: buildRecurRule(form.recurrence),
      reminderMinutes: form.reminderMinutes,
    }
    if (form.id) {
      await calendarApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await calendarApi.create({ ...payload, calendarId: form.calendarId })
      ElMessage.success('已创建')
    }
    formVisible.value = false
    detailVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref(null)
const detailOccurrenceDate = ref(null)
const canEdit = computed(
  () => detail.value && String(detail.value.organizerId) === String(userStore.userId),
)

async function openDetail(s) {
  detailOccurrenceDate.value = s.occurrenceDate || null
  detail.value = await calendarApi.get(s.id)
  detailVisible.value = true
}

// 取消循环日程的「这一次」
async function cancelThisOccurrence() {
  await ElMessageBox.confirm('仅取消该次循环日程？', '提示', { type: 'warning' })
  await calendarApi.addException(detail.value.id, {
    occurDate: detailOccurrenceDate.value,
    action: 'cancel',
  })
  ElMessage.success('已取消该次')
  detailVisible.value = false
  await load()
}

function openEdit() {
  const d = detail.value
  form.id = d.id
  form.title = d.title
  form.startTime = dayjs(d.startTime).format('YYYY-MM-DDTHH:mm:ss')
  form.endTime = dayjs(d.endTime).format('YYYY-MM-DDTHH:mm:ss')
  form.allDay = !!d.allDay
  form.allowFeedback = !!d.allowFeedback
  form.location = d.location || ''
  form.description = d.description || ''
  form.participantUserIds = (d.participants || [])
    .filter((p) => p.role !== 'organizer' && p.userId)
    .map((p) => p.userId)
  form.resourceIds = d.resourceIds || []
  form.recurrence = parseFreq(d.recurRule)
  form.reminderMinutes = d.reminderMinutes || []
  formVisible.value = true
}

async function remove() {
  await ElMessageBox.confirm('确认删除该日程？', '提示', { type: 'warning' })
  await calendarApi.remove(detail.value.id)
  ElMessage.success('已删除')
  detailVisible.value = false
  await load()
}

// ===== 排期小助手 =====
const slotDialogVisible = ref(false)
const slotLoading = ref(false)
const slotsSearched = ref(false)
const slots = ref([])
const slotForm = reactive({ userIds: [], date: '', durationMinutes: 60 })

function openSlotAssistant() {
  const me = Number(userStore.userId)
  slotForm.userIds = Array.from(new Set([me, ...(form.participantUserIds || [])])).filter(Boolean)
  slotForm.date = dayjs(form.startTime || dayjs()).format('YYYY-MM-DD')
  slotForm.durationMinutes = 60
  slots.value = []
  slotsSearched.value = false
  slotDialogVisible.value = true
}

async function searchSlots() {
  if (!slotForm.userIds.length || !slotForm.date) {
    ElMessage.warning('请选择参选人与日期')
    return
  }
  slotLoading.value = true
  try {
    slots.value = await calendarApi.findSlots({
      userIds: slotForm.userIds,
      date: slotForm.date,
      durationMinutes: slotForm.durationMinutes,
    })
    slotsSearched.value = true
  } finally {
    slotLoading.value = false
  }
}

function pickSlot(slot) {
  form.startTime = dayjs(slot.start).format('YYYY-MM-DDTHH:mm:ss')
  form.endTime = dayjs(slot.start).add(slotForm.durationMinutes, 'minute').format('YYYY-MM-DDTHH:mm:ss')
  slotDialogVisible.value = false
  ElMessage.success('已填入开始/结束时间')
}

async function doRsvp(status) {
  await calendarApi.rsvp(detail.value.id, status)
  ElMessage.success('反馈已提交')
  detail.value = await calendarApi.get(detail.value.id)
}

onMounted(async () => {
  ;[calendars.value, members.value, resources.value] = await Promise.all([
    calendarApi.listCalendars(),
    fetchMembers(),
    calendarApi.listResources(),
  ])
  await load()
})
</script>

<style scoped>
.mido-calendar {
  padding: 16px;
}
.mido-calendar__bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.mido-calendar__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}
.mido-calendar__nav {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.mido-calendar__period {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.mido-calendar__body {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  min-height: 60vh;
}
.mido-month__head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.mido-month__head span {
  padding: 8px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.mido-month__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.mido-month__cell {
  min-height: 100px;
  border-right: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 4px 6px;
  cursor: pointer;
}
.mido-month__cell.is-other {
  background: var(--el-fill-color-light);
}
.mido-month__cell.is-other .mido-month__date {
  color: var(--el-text-color-disabled);
}
.mido-month__date {
  font-size: 13px;
  text-align: right;
}
.mido-month__cell.is-today .mido-month__date {
  color: var(--el-color-primary);
  font-weight: 700;
}
.mido-event {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 1px 2px;
  border-radius: 3px;
  cursor: pointer;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.mido-event:hover {
  background: var(--mido-hover-bg);
}
.mido-event__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  flex-shrink: 0;
}
.mido-event--task .mido-event__text {
  color: var(--el-color-warning);
}
.mido-event__dot--task {
  background: var(--el-color-warning);
  border-radius: 2px;
}
.mido-event--busy .mido-event__text {
  color: var(--el-text-color-secondary);
}
.mido-event__dot--busy {
  background: var(--el-text-color-secondary);
}
.mido-event__text {
  overflow: hidden;
  text-overflow: ellipsis;
}
.mido-list__day {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.mido-list__date {
  font-weight: 600;
  margin-bottom: 8px;
}
.mido-list__date.is-today {
  color: var(--el-color-primary);
}
.mido-list__empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.mido-detail__title {
  margin: 0 0 8px;
}
.mido-detail__time {
  color: var(--el-text-color-secondary);
}
.mido-detail__desc {
  white-space: pre-wrap;
}
.mido-detail__pt {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}
.mido-detail__rsvp {
  display: flex;
  gap: 8px;
}
.mido-slots {
  margin-top: 12px;
  max-height: 240px;
  overflow-y: auto;
}
.mido-slot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
}
.mido-slot:hover {
  background: var(--mido-hover-bg);
}
</style>
