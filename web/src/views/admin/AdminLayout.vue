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
        <el-menu-item index="/admin/approval-flows">
          <el-icon><Stamp /></el-icon><span>审批流设计器</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <section class="admin__content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { User, Avatar, OfficeBuilding, Connection, Stamp } from '@element-plus/icons-vue'
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
