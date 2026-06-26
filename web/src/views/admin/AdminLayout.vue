<template>
  <div class="admin">
    <aside class="admin__nav">
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin/members">
          <el-icon><User /></el-icon><span>成员管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/roles">
          <el-icon><Avatar /></el-icon><span>角色管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/depts">
          <el-icon><OfficeBuilding /></el-icon><span>部门树</span>
        </el-menu-item>
        <el-menu-item index="/admin/org">
          <el-icon><Connection /></el-icon><span>组织架构</span>
        </el-menu-item>
        <el-menu-item index="/admin/project-types">
          <el-icon><CollectionTag /></el-icon><span>项目类型</span>
        </el-menu-item>
        <el-menu-item index="/admin/project-templates">
          <el-icon><Files /></el-icon><span>项目模板</span>
        </el-menu-item>
        <el-menu-item index="/admin/project-roles">
          <el-icon><Avatar /></el-icon><span>项目角色</span>
        </el-menu-item>
        <el-menu-item index="/admin/workspace-nav">
          <el-icon><Operation /></el-icon><span>导航配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/workspace-page">
          <el-icon><Memo /></el-icon><span>页面配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/npss-settings">
          <el-icon><Histogram /></el-icon><span>NPSS 评价设置</span>
        </el-menu-item>
        <el-menu-item index="/admin/approval-flows">
          <el-icon><Stamp /></el-icon><span>审批流</span>
        </el-menu-item>
        <el-menu-item index="/admin/change-policies">
          <el-icon><Switch /></el-icon><span>变更策略</span>
        </el-menu-item>
        <el-menu-item index="/admin/fields">
          <el-icon><Memo /></el-icon><span>自定义字段</span>
        </el-menu-item>
        <el-menu-item index="/admin/data-sources">
          <el-icon><Collection /></el-icon><span>数据源</span>
        </el-menu-item>
        <el-menu-item index="/admin/work-item-types">
          <el-icon><Files /></el-icon><span>工作项类型</span>
        </el-menu-item>
        <el-menu-item index="/admin/relation-defs">
          <el-icon><Share /></el-icon><span>关联关系</span>
        </el-menu-item>
        <el-menu-item index="/admin/statuses">
          <el-icon><Flag /></el-icon><span>状态库</span>
        </el-menu-item>
        <el-menu-item index="/admin/priority-modes">
          <el-icon><Sort /></el-icon><span>优先级模式</span>
        </el-menu-item>
        <el-menu-item index="/admin/audit-logs">
          <el-icon><Tickets /></el-icon><span>操作日志</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.hasFeature('openapi')" index="/admin/apikeys">
          <el-icon><Key /></el-icon><span>开放平台</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <section class="admin__content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { User, Avatar, OfficeBuilding, Connection, Stamp, CollectionTag, Key, Switch, Memo, Histogram, Tickets, Collection, Flag, Sort, Files, Share } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
</script>

<style scoped>
.admin {
  display: flex;
  gap: var(--mido-space-4);
  height: 100%;
}

.admin__nav {
  width: var(--mido-admin-nav-width);
  background-color: var(--el-bg-color);
  border-radius: var(--mido-radius-md);
  box-shadow: var(--mido-shadow-card);
  overflow: hidden;
}

.admin__content {
  flex: 1;
  min-width: 0;
}
</style>
