<template>
  <!-- 通用弹窗式选人器（design-system §5.2）：
       触发区类 el-select 外观；点击打开对话框，左侧组织架构树、右侧成员列表（搜索 + 分页），底部已选区。
       v-model 契约：单选 String、多选 Array<String>；雪花 ID 以字符串透传防精度丢失。 -->
  <div class="us" :class="{ 'is-disabled': disabled }">
    <div class="us__trigger" tabindex="0" @click="open" @keydown.enter.prevent="open">
      <template v-if="hasValue">
        <span v-if="!multiple" class="us__single">
          <span class="us__avatar" :style="avatarStyle(selectedUsers[0])">{{ initial(selectedUsers[0]) }}</span>
          <span class="us__name">{{ label(selectedUsers[0]) }}</span>
        </span>
        <span v-else class="us__tags">
          <el-tag
            v-for="u in selectedUsers"
            :key="u.id"
            closable
            disable-transitions
            size="small"
            @close.stop="remove(u.id)"
          >{{ label(u) }}</el-tag>
        </span>
      </template>
      <span v-else class="us__placeholder">{{ placeholder }}</span>
      <el-icon v-if="hasValue && clearable && !disabled" class="us__clear" @click.stop="clearAll"><CircleClose /></el-icon>
      <el-icon class="us__arrow"><ArrowDown /></el-icon>
    </div>

    <el-dialog
      v-model="visible"
      title="选择人员"
      width="720px"
      append-to-body
      :close-on-click-modal="false"
      class="us-dialog"
    >
      <div class="us-body">
        <!-- 左：组织架构树 -->
        <div class="us-body__tree">
          <div
            class="us-tree-all"
            :class="{ 'is-active': activeDept === null }"
            @click="selectDept(null)"
          >全部成员</div>
          <el-tree
            :data="deptTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :expand-on-click-node="false"
            highlight-current
            @node-click="(n) => selectDept(n.id)"
          />
        </div>
        <!-- 右：搜索 + 成员列表 + 分页 -->
        <div class="us-body__list">
          <el-input
            v-model="keyword"
            placeholder="搜索姓名 / 账号"
            clearable
            :prefix-icon="Search"
            @input="onSearch"
          />
          <div v-loading="loading" class="us-list">
            <div
              v-for="u in users"
              :key="u.id"
              class="us-row"
              :class="{ 'is-picked': picked.has(String(u.id)) }"
              @click="toggle(u)"
            >
              <span class="us__avatar" :style="avatarStyle(u)">{{ initial(u) }}</span>
              <span class="us-row__main">
                <span class="us-row__name">{{ u.name || u.username }}</span>
                <span class="us-row__meta">{{ u.username }}<template v-if="u.jobLevel"> · {{ u.jobLevel }}</template></span>
              </span>
              <el-icon v-if="picked.has(String(u.id))" class="us-row__check"><Check /></el-icon>
            </div>
            <el-empty v-if="!loading && users.length === 0" description="无匹配成员" :image-size="60" />
          </div>
          <el-pagination
            v-if="total > pageSize"
            v-model:current-page="page"
            small
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="total"
            @current-change="loadUsers"
          />
        </div>
      </div>

      <template #footer>
        <div class="us-footer">
          <span class="us-footer__count">已选 {{ picked.size }} 人</span>
          <span>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="primary" @click="confirm">确定</el-button>
          </span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowDown, Check, CircleClose, Search } from '@element-plus/icons-vue'
import { deptApi, fetchMembers, userApi } from '@/api/org'

const props = defineProps({
  // 单选：String/Number；多选：Array
  modelValue: { type: [String, Number, Array], default: null },
  multiple: { type: Boolean, default: false },
  placeholder: { type: String, default: '请选择人员' },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'change'])

// 全量成员缓存：仅用于把已选 ID 解析成姓名/职级展示（列表本身走分页接口）。
const memberCache = ref([])
const cacheById = computed(() => {
  const m = new Map()
  memberCache.value.forEach((u) => m.set(String(u.id), u))
  return m
})

const ids = computed(() =>
  props.multiple
    ? (Array.isArray(props.modelValue) ? props.modelValue.map(String) : [])
    : (props.modelValue != null && props.modelValue !== '' ? [String(props.modelValue)] : []),
)
const hasValue = computed(() => ids.value.length > 0)
const selectedUsers = computed(() => ids.value.map((id) => cacheById.value.get(id) || { id, name: `用户#${id}` }))

const label = (u) => u?.name || u?.username || (u?.id ? `用户#${u.id}` : '')
const initial = (u) => (u?.name || u?.username || '?').trim().charAt(0)
// 头像底色：按 ID 取色盘，稳定且与 design-system 主色系协调。
const AVATAR_COLORS = ['#3D6EFF', '#11A2C7', '#2BA471', '#7C5CFF', '#E37318', '#D54941']
const avatarStyle = (u) => {
  const n = Number(u?.id) || String(u?.id || '').length
  return { backgroundColor: AVATAR_COLORS[Math.abs(n) % AVATAR_COLORS.length] }
}

// ===== 对话框 =====
const visible = ref(false)
const deptTree = ref([])
const activeDept = ref(null)
const keyword = ref('')
const users = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const picked = ref(new Map()) // id(String) -> user

function open() {
  if (props.disabled) return
  // 以当前值初始化已选
  picked.value = new Map(selectedUsers.value.map((u) => [String(u.id), u]))
  visible.value = true
  if (deptTree.value.length === 0) loadDepts()
  page.value = 1
  loadUsers()
}

async function loadDepts() {
  try {
    deptTree.value = (await deptApi.tree()) || []
  } catch {
    deptTree.value = []
  }
}

function selectDept(id) {
  activeDept.value = id
  page.value = 1
  loadUsers()
}

let searchTimer = null
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadUsers()
  }, 300)
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.query({
      page: page.value,
      size: pageSize,
      username: keyword.value || undefined,
      deptId: activeDept.value || undefined,
    })
    users.value = res.list || []
    total.value = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

function toggle(u) {
  const id = String(u.id)
  if (props.multiple) {
    if (picked.value.has(id)) picked.value.delete(id)
    else picked.value.set(id, u)
  } else {
    picked.value = new Map([[id, u]])
  }
}

function confirm() {
  // 把本次选择并入缓存源，确保确认后触发区能解析出姓名（cacheById 为派生，需写源 ref）
  const known = cacheById.value
  picked.value.forEach((u) => {
    if (u && u.name && !known.has(String(u.id))) memberCache.value.push(u)
  })
  const list = [...picked.value.keys()]
  const val = props.multiple ? list : (list[0] ?? null)
  emit('update:modelValue', val)
  emit('change', val)
  visible.value = false
}

function remove(id) {
  if (props.disabled) return
  if (props.multiple) {
    const next = ids.value.filter((x) => x !== String(id))
    emit('update:modelValue', next)
    emit('change', next)
  } else {
    clearAll()
  }
}

function clearAll() {
  const val = props.multiple ? [] : null
  emit('update:modelValue', val)
  emit('change', val)
}

// 预加载成员缓存以解析现有值；值变更时若有未知 ID 再补一次。
async function ensureCache() {
  if (memberCache.value.length === 0) {
    try {
      memberCache.value = await fetchMembers()
    } catch {
      memberCache.value = []
    }
  }
}
onMounted(ensureCache)
watch(() => props.modelValue, ensureCache)
</script>

<style scoped>
.us {
  width: 100%;
}
.us__trigger {
  display: flex;
  align-items: center;
  gap: var(--mido-space-1);
  min-height: 32px;
  padding: 2px 28px 2px var(--mido-space-2);
  position: relative;
  border: var(--mido-border-width) solid var(--el-border-color);
  border-radius: var(--mido-radius-md);
  background: var(--el-bg-color);
  cursor: pointer;
  transition: border-color var(--mido-duration) var(--mido-ease);
}
.us__trigger:hover {
  border-color: var(--el-border-color-hover, var(--el-text-color-placeholder));
}
.us.is-disabled .us__trigger {
  cursor: not-allowed;
  background: var(--el-fill-color-light);
}
.us__single {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.us__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-1);
  padding: 2px 0;
}
.us__placeholder {
  color: var(--el-text-color-placeholder);
}
.us__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  font-size: var(--mido-font-size-caption);
  flex: none;
}
.us__name {
  color: var(--el-text-color-primary);
}
.us__clear,
.us__arrow {
  position: absolute;
  right: var(--mido-space-2);
  color: var(--el-text-color-placeholder);
}
.us__clear {
  right: 26px;
  cursor: pointer;
}
.us__clear:hover {
  color: var(--el-text-color-regular);
}

.us-body {
  display: flex;
  gap: var(--mido-space-4);
  height: 380px;
}
.us-body__tree {
  width: 200px;
  flex: none;
  overflow: auto;
  border-right: var(--mido-border-width) solid var(--el-border-color-light);
  padding-right: var(--mido-space-2);
}
.us-tree-all {
  padding: var(--mido-space-1) var(--mido-space-2);
  border-radius: var(--mido-radius-sm);
  cursor: pointer;
  margin-bottom: var(--mido-space-1);
}
.us-tree-all.is-active,
.us-tree-all:hover {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.us-body__list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  min-width: 0;
}
.us-list {
  flex: 1;
  overflow: auto;
}
.us-row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2);
  border-radius: var(--mido-radius-sm);
  cursor: pointer;
}
.us-row:hover {
  background: var(--el-fill-color-light);
}
.us-row.is-picked {
  background: var(--el-color-primary-light-9);
}
.us-row__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.us-row__name {
  color: var(--el-text-color-primary);
}
.us-row__meta {
  font-size: var(--mido-font-size-caption);
  color: var(--el-text-color-secondary);
}
.us-row__check {
  margin-left: auto;
  color: var(--el-color-primary);
}
.us-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.us-footer__count {
  color: var(--el-text-color-secondary);
  font-size: var(--mido-font-size-secondary);
}
</style>
