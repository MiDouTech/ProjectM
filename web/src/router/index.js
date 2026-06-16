import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

// 顶层导航占位路由（design-system §4 / architecture-overview §1.2）
export const navRoutes = [
  { path: 'workbench', name: 'workbench', meta: { title: '工作台', icon: 'Monitor' }, component: () => import('@/views/Workbench.vue') },
  { path: 'project', name: 'project', meta: { title: '项目', icon: 'Folder' }, component: () => import('@/views/Project.vue') },
  { path: 'goal', name: 'goal', meta: { title: '目标', icon: 'Aim' }, component: () => import('@/views/Goal.vue') },
  { path: 'approval', name: 'approval', meta: { title: '审批', icon: 'Stamp' }, component: () => import('@/views/Approval.vue') },
  { path: 'report', name: 'report', meta: { title: '报表', icon: 'DataAnalysis' }, component: () => import('@/views/Report.vue') },
  { path: 'doc', name: 'doc', meta: { title: '文档', icon: 'Document' }, component: () => import('@/views/Doc.vue') },
  { path: 'admin', name: 'admin', meta: { title: '管理后台', icon: 'Setting' }, component: () => import('@/views/Admin.vue') },
]

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/workbench',
    children: navRoutes,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
