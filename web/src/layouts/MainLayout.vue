<template>
  <div class="mido-layout">
    <!-- 顶栏 TopBar（design-system §4，高度走 --mido-topbar-height）-->
    <header class="mido-topbar">
      <div class="mido-topbar__brand">
        <el-icon class="mido-topbar__toggle" @click="collapsed = !collapsed">
          <component :is="collapsed ? Expand : Fold" />
        </el-icon>
        <img v-if="logoOk" class="mido-topbar__logo" src="/logo_竖_蓝色.png"
          alt="米多 · 通用项目管理系统" @error="logoOk = false" />
        <el-icon v-else class="mido-topbar__logo-fallback"><Grid /></el-icon>
        <span v-show="!collapsed" class="mido-h2">项目管理</span>
      </div>
      <!-- 二级导航并入顶栏中段（design-system §4）：各模块的 WorkspaceShell 经 Teleport 注入此处 -->
      <div id="mido-topbar-nav" class="mido-topbar__nav" />
      <div class="mido-topbar__actions">
        <!-- 统一消息入口：系统消息未读 + 未看平台公告，合并角标，点击进消息中心 -->
        <el-badge :value="totalUnread" :max="99" :hidden="!totalUnread" class="mido-topbar__bell">
          <el-icon class="mido-topbar__icon" role="button" tabindex="0"
            :aria-label="totalUnread ? `消息中心，${totalUnread} 条未读` : '消息中心'"
            @click="goNotifications" @keydown.enter="goNotifications" @keydown.space.prevent="goNotifications">
            <Bell />
          </el-icon>
        </el-badge>
        <el-dropdown @command="onUserCommand">
          <el-avatar class="mido-topbar__avatar" :src="myAvatarUrl">{{ myInitial }}</el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="mido-body">
      <!-- 左侧深色主导航（design-system §4）-->
      <aside class="mido-nav" :class="{ 'mido-nav--collapsed': collapsed }">
        <el-menu
          :default-active="activeMenu"
          class="mido-nav__menu"
          :collapse="collapsed"
          router
          background-color="transparent"
        >
          <el-menu-item
            v-for="item in visibleNavItems"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
        <!-- 管理后台：独立全屏布局，新标签打开（不占主应用正文宽度）-->
        <div class="mido-nav__footer">
          <button type="button" class="mido-nav__admin" :title="collapsed ? '管理后台（新标签打开）' : ''"
            @click="openAdmin">
            <el-icon><Setting /></el-icon>
            <span v-show="!collapsed">管理后台</span>
          </button>
        </div>
      </aside>

      <!-- 主内容区（视图容器）-->
      <main class="mido-main">
        <router-view />
      </main>
    </div>

    <!-- 修改密码（首登默认密码须改；自助校验原密码） -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="off" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Grid, Fold, Expand, Setting } from '@element-plus/icons-vue'
import { navItems } from '@/router'
import { useUserStore } from '@/store/user'
import { notificationApi } from '@/api/collab'
import { userApi } from '@/api/org'
import { attachmentApi } from '@/api/attachment'
import { appApi } from '@/api/app'
import { unseenCount } from '@/utils/announcements'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// 高亮顶层导航（嵌套子路由如 /admin/members 也命中 /admin）
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'workbench'))

// 顶级导航 → 功能码映射；未列出的项无对应功能码，始终显示。
// features 为空（未取到）时 hasFeature 走 fail-open 默认全显示。
const NAV_FEATURE_MAP = {
  '/goal': 'okr',
  '/doc': 'doc',
  '/report': 'report',
}
const visibleNavItems = computed(() =>
  navItems.filter((item) => {
    const code = NAV_FEATURE_MAP[item.path]
    return !code || userStore.hasFeature(code)
  }),
)

// 平台公告（当前生效）：用于计算未看数并入消息徽标
const announcements = ref([])
const unseenAnno = ref(0)
function recomputeUnseenAnno() {
  unseenAnno.value = unseenCount(announcements.value, userStore.userId)
}
async function loadAnnouncements() {
  try {
    announcements.value = (await appApi.announcements()) || []
  } catch {
    announcements.value = []
  }
  recomputeUnseenAnno()
}

// 顶栏未读数：进入应用 / 路由切换时刷新，并定时轮询；页面不可见时暂停以省资源。
// 消息徽标 = 系统消息未读 + 未看平台公告（统一入口）。
const unread = ref(0)
const totalUnread = computed(() => unread.value + unseenAnno.value)
const POLL_MS = 30000
let pollTimer = null
async function loadUnread() {
  try {
    unread.value = await notificationApi.unreadCount()
  } catch {
    unread.value = 0
  }
}
function goNotifications() {
  router.push('/notifications')
}
// 管理后台在新标签打开独立全屏布局，避免双左导航挤压正文
function openAdmin() {
  window.open('/admin', '_blank')
}

// 当前登录用户头像（顶栏）：有头像取限时 URL，否则回落姓名首字
const myAvatarUrl = ref('')
const myInitial = ref('M')
async function loadMe() {
  if (!useUserStore().isLogin) return
  try {
    const me = await userApi.me()
    myInitial.value = (me.name || me.username || 'M').charAt(0)
    myAvatarUrl.value = me.avatar ? await attachmentApi.downloadUrl(me.avatar) : ''
  } catch {
    myAvatarUrl.value = ''
  }
}
function startPoll() {
  stopPoll()
  pollTimer = setInterval(loadUnread, POLL_MS)
}
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
// 标签页可见性：可见则刷新并轮询，隐藏则停轮询
function onVisibility() {
  if (document.hidden) {
    stopPoll()
  } else {
    loadUnread()
    startPoll()
  }
}
// 路由切换（尤其从消息中心返回）时刷新系统未读，并重算公告未看数，保持徽标同步
watch(() => route.path, () => {
  loadUnread()
  recomputeUnseenAnno()
})

// 顶栏 logo：缺图时优雅回落到 Grid 图标
const logoOk = ref(true)

// 响应式导航：<1280 自动收起为图标态（design-system §9），亦可手动切换
const NARROW = 1280
const collapsed = ref(false)
function syncByWidth() {
  collapsed.value = window.innerWidth < NARROW
}
onMounted(() => {
  syncByWidth()
  window.addEventListener('resize', syncByWidth)
  document.addEventListener('visibilitychange', onVisibility)
  loadUnread()
  startPoll()
  loadMe()
  userStore.fetchFeatures()
  loadAnnouncements()
})
onUnmounted(() => {
  window.removeEventListener('resize', syncByWidth)
  document.removeEventListener('visibilitychange', onVisibility)
  stopPoll()
})

// ===== 修改密码 =====
const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '新密码长度需为 8-64 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        v === pwdForm.newPassword ? cb() : cb(new Error('两次输入的新密码不一致')),
      trigger: 'blur',
    },
  ],
}

function openPasswordDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
  pwdFormRef.value?.clearValidate?.()
}

async function submitPassword() {
  await pwdFormRef.value.validate()
  pwdSubmitting.value = true
  try {
    await userApi.changeMyPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    pwdDialogVisible.value = false
    useUserStore().clearToken()
    router.push('/login')
  } finally {
    pwdSubmitting.value = false
  }
}

function onUserCommand(command) {
  if (command === 'password') {
    openPasswordDialog()
  } else if (command === 'logout') {
    useUserStore().clearToken()
    router.push('/login')
  }
}
</script>

<style scoped>
.mido-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mido-topbar {
  display: flex;
  align-items: center;
  height: var(--mido-topbar-height);
  padding: 0 var(--mido-space-4);
  background-color: var(--el-bg-color);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  z-index: var(--mido-z-nav);
}

.mido-topbar__brand {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  color: var(--el-color-primary);
}

.mido-topbar__toggle {
  font-size: var(--mido-font-size-h2);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

/* 顶栏 logo：限高自适应，竖版图按顶栏行高收纳 */
.mido-topbar__logo {
  height: var(--mido-space-6);
  width: auto;
  object-fit: contain;
  display: block;
}
.mido-topbar__logo-fallback {
  font-size: var(--mido-font-size-h1);
}

/* 顶栏中段：承载当前模块的二级导航（Teleport 目标）。空时不显分隔线，避免裸竖线 */
.mido-topbar__nav {
  flex: 1;
  min-width: 0;
  align-self: stretch;
  display: flex;
  align-items: stretch;
  overflow: hidden;
  margin-left: var(--mido-space-4);
}
.mido-topbar__nav:not(:empty) {
  padding-left: var(--mido-space-4);
  border-left: var(--mido-border-width) solid var(--el-border-color-lighter);
}

.mido-topbar__actions {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
}

/* 通知铃铛：el-badge 默认 inline-block 基线对齐会让图标偏上，改 flex 垂直居中 */
.mido-topbar__bell {
  display: flex;
  align-items: center;
}

.mido-topbar__icon {
  display: flex;
  font-size: var(--mido-font-size-h2);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

.mido-topbar__avatar {
  width: var(--mido-space-6);
  height: var(--mido-space-6);
  background-color: var(--el-color-primary);
  cursor: pointer;
}

.mido-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.mido-nav {
  width: var(--mido-nav-width);
  background-color: var(--mido-nav-bg);
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  transition: width var(--mido-duration) var(--mido-ease);
}
/* 主菜单占满、可滚；管理后台入口固定底部 */
.mido-nav__menu {
  flex: 1;
  overflow-y: auto;
}
.mido-nav__footer {
  flex: none;
  border-top: var(--mido-border-width) solid var(--mido-nav-active-bg);
}
.mido-nav__admin {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  width: 100%;
  height: var(--mido-space-6);
  padding: 0 20px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--mido-nav-text);
  font-size: var(--mido-font-size-body);
  transition: background-color var(--mido-duration) var(--mido-ease),
    color var(--mido-duration) var(--mido-ease);
}
.mido-nav__admin:hover {
  background-color: var(--mido-nav-active-bg);
  color: var(--mido-nav-text-active);
}
.mido-nav--collapsed .mido-nav__admin {
  justify-content: center;
  padding: 0;
}
.mido-nav--collapsed {
  width: var(--mido-nav-width-collapsed);
}

/* 收起态：el-menu collapse 自身宽 64px，与导航容器对齐 */
.mido-nav__menu {
  border-right: none;
}
.mido-nav__menu:not(.el-menu--collapse) {
  width: var(--mido-nav-width);
}

@media (prefers-reduced-motion: reduce) {
  .mido-nav {
    transition: none;
  }
}

/* 深色导航的 Element Plus 菜单换肤（仅用 tokens）*/
.mido-nav__menu :deep(.el-menu-item) {
  color: var(--mido-nav-text);
  transition: background-color var(--mido-duration) var(--mido-ease),
    color var(--mido-duration) var(--mido-ease);
}

/* active：底色 + 3px 主色左强调条（Worktile 式，识别更利落）*/
.mido-nav__menu :deep(.el-menu-item.is-active) {
  color: var(--mido-nav-text-active);
  background-color: var(--mido-nav-active-bg);
  box-shadow: inset var(--mido-space-1) 0 0 var(--el-color-primary);
}

.mido-nav__menu :deep(.el-menu-item:hover) {
  background-color: var(--mido-nav-active-bg);
}

.mido-main {
  flex: 1;
  min-width: 0;
  padding: var(--mido-space-5);
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}
</style>
