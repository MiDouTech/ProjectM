import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/store/user'
import { useOpsUserStore } from '@/store/opsUser'

// 顶层主导航（design-system §4 / architecture-overview §1.2）
export const navItems = [
  { path: '/workbench', title: '工作台', icon: 'Monitor' },
  { path: '/project', title: '项目', icon: 'Folder' },
  { path: '/goal', title: '目标', icon: 'Aim' },
  { path: '/approval', title: '审批', icon: 'Stamp' },
  { path: '/change', title: '变更中心', icon: 'RefreshRight' },
  { path: '/report', title: '报表', icon: 'DataAnalysis' },
  { path: '/doc', title: '文档', icon: 'Document' },
  { path: '/admin', title: '管理后台', icon: 'Setting' },
]

// 平台运营后台侧导航（独立于租户应用 navItems）
export const opsNavItems = [
  { path: '/ops/dashboard', title: '运营概览', icon: 'DataLine' },
  { path: '/ops/tenants', title: '租户管理', icon: 'OfficeBuilding' },
  { path: '/ops/plans', title: '套餐管理', icon: 'Goods' },
  { path: '/ops/admins', title: '运营账号', icon: 'UserFilled' },
  { path: '/ops/audit', title: '审计日志', icon: 'Tickets' },
]

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
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
      { path: 'change', component: () => import('@/views/ChangeCenter.vue') },
      { path: 'report', component: () => import('@/views/Report.vue') },
      { path: 'doc', component: () => import('@/views/Doc.vue') },
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
  // 公开分享页：匿名可访问，跳过登录校验
  if (to.name === 'publicDoc') {
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
