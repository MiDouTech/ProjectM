import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/store/user'

// 顶层主导航（design-system §4 / architecture-overview §1.2）
export const navItems = [
  { path: '/workbench', title: '工作台', icon: 'Monitor' },
  { path: '/project', title: '项目', icon: 'Folder' },
  { path: '/goal', title: '目标', icon: 'Aim' },
  { path: '/approval', title: '审批', icon: 'Stamp' },
  { path: '/report', title: '报表', icon: 'DataAnalysis' },
  { path: '/doc', title: '文档', icon: 'Document' },
  { path: '/admin', title: '管理后台', icon: 'Setting' },
]

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
  {
    path: '/',
    component: MainLayout,
    redirect: '/workbench',
    children: [
      { path: 'workbench', component: () => import('@/views/Workbench.vue') },
      { path: 'project', component: () => import('@/views/project/ProjectListView.vue') },
      { path: 'project/:projectId/tasks', component: () => import('@/views/task/TaskWorkspaceView.vue') },
      { path: 'goal', component: () => import('@/views/Goal.vue') },
      { path: 'approval', component: () => import('@/views/approval/ApprovalView.vue') },
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
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && userStore.token) {
    return { path: '/' }
  }
  return true
})

export default router
