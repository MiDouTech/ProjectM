import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/store/user'
import { useOpsUserStore } from '@/store/opsUser'

// 顶层主导航（design-system §4 / architecture-overview §1.2）
export const navItems = [
  { path: '/workbench', title: '工作台', icon: 'Monitor' },
  { path: '/project', title: '项目', icon: 'Folder' },
  { path: '/goal', title: '目标', icon: 'Aim' },
  { path: '/approval', title: '审批中心', icon: 'Stamp' },
  { path: '/report', title: '报表', icon: 'DataAnalysis' },
  { path: '/doc', title: '文档', icon: 'Document' },
  { path: '/calendar', title: '日历', icon: 'Calendar' },
  { path: '/briefing', title: '简报', icon: 'Notebook' },
  { path: '/admin', title: '管理后台', icon: 'Setting' },
]

// 平台运营后台侧导航（独立于租户应用 navItems）
export const opsNavItems = [
  { path: '/ops/dashboard', title: '运营概览', icon: 'DataLine' },
  { path: '/ops/tenants', title: '租户管理', icon: 'OfficeBuilding' },
  { path: '/ops/plans', title: '套餐管理', icon: 'Goods' },
  { path: '/ops/revenue', title: '收入台账', icon: 'Coin' },
  { path: '/ops/announcements', title: '公告', icon: 'Bell' },
  { path: '/ops/admins', title: '运营账号', icon: 'UserFilled' },
  { path: '/ops/audit', title: '审计日志', icon: 'Tickets' },
]

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
  { path: '/wecom-callback', name: 'wecomCallback', component: () => import('@/views/auth/WecomCallbackView.vue') },
  // ===== 平台运营后台（独立登录/布局，前缀 /ops）=====
  { path: '/ops/login', name: 'opsLogin', component: () => import('@/views/ops/OpsLoginView.vue') },
  {
    path: '/ops',
    component: () => import('@/layouts/OpsLayout.vue'),
    redirect: '/ops/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/ops/DashboardView.vue') },
      { path: 'tenants', component: () => import('@/views/ops/TenantManage.vue') },
      { path: 'plans', component: () => import('@/views/ops/PlanManage.vue') },
      { path: 'revenue', component: () => import('@/views/ops/RevenueView.vue') },
      { path: 'announcements', component: () => import('@/views/ops/AnnouncementManage.vue') },
      { path: 'admins', component: () => import('@/views/ops/AdminManage.vue') },
      { path: 'audit', component: () => import('@/views/ops/AuditView.vue') },
    ],
  },
  { path: '/share/:token', name: 'publicDoc', component: () => import('@/views/PublicDocView.vue') },
  {
    path: '/',
    component: MainLayout,
    redirect: '/workbench',
    children: [
      { path: 'workbench', component: () => import('@/views/Workbench.vue') },
      { path: 'notifications', component: () => import('@/views/NotificationListView.vue') },
      { path: 'project', component: () => import('@/views/project/ProjectListView.vue') },
      { path: 'project/:projectId', component: () => import('@/views/project/ProjectWorkspaceView.vue') },
      { path: 'project/:projectId/tasks', component: () => import('@/views/task/TaskWorkspaceView.vue') },
      { path: 'project/:projectId/task/:taskId', component: () => import('@/views/task/TaskDetailView.vue') },
      { path: 'project/:projectId/stakeholders', component: () => import('@/views/stakeholder/StakeholderView.vue') },
      { path: 'goal', component: () => import('@/views/Goal.vue') },
      { path: 'approval', component: () => import('@/views/approval/ApprovalView.vue') },
      // 变更中心已并入「审批中心」的变更台账 Tab；保留旧路径重定向兼容书签/深链
      { path: 'change', redirect: { path: '/approval', query: { tab: 'change' } } },
      { path: 'report', component: () => import('@/views/Report.vue') },
      { path: 'doc', component: () => import('@/views/Doc.vue') },
      { path: 'calendar', component: () => import('@/views/calendar/CalendarView.vue') },
      { path: 'briefing', component: () => import('@/views/briefing/BriefingView.vue') },
      {
        path: 'admin',
        component: () => import('@/views/admin/AdminLayout.vue'),
        redirect: '/admin/members',
        children: [
          { path: 'members', component: () => import('@/views/admin/MemberManage.vue') },
          { path: 'roles', component: () => import('@/views/admin/RoleManage.vue') },
          { path: 'depts', component: () => import('@/views/admin/DeptTree.vue') },
          { path: 'org', component: () => import('@/views/admin/OrgStructure.vue') },
          { path: 'project-types', component: () => import('@/views/admin/ProjectTypeManage.vue') },
          { path: 'approval-flows', component: () => import('@/views/admin/ApprovalFlowDesigner.vue') },
          { path: 'change-policies', component: () => import('@/views/admin/ChangePolicyManage.vue') },
          { path: 'fields', component: () => import('@/views/admin/FieldDefManage.vue') },
          { path: 'apikeys', component: () => import('@/views/admin/ApiKeyManage.vue') },
        ],
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 认证守卫：未登录跳登录页
router.beforeEach((to) => {
  // ===== 平台运营后台分支（独立登录态，最前处理；不触碰租户逻辑）=====
  if (to.path.startsWith('/ops')) {
    const opsStore = useOpsUserStore()
    if (to.path === '/ops/login') {
      return opsStore.token ? { path: '/ops' } : true
    }
    return opsStore.token ? true : { path: '/ops/login' }
  }

  const userStore = useUserStore()
  // 公开分享页 / 企微 SSO 回调：匿名可访问，跳过登录校验
  if (to.name === 'publicDoc' || to.name === 'wecomCallback') {
    return true
  }
  if (to.path !== '/login' && !userStore.token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && userStore.token) {
    return { path: '/' }
  }
  return true
})

export default router
