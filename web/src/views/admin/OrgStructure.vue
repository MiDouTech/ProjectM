<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">组织架构</h2>
      <el-tooltip content="企业微信同步将在 P2 激活" placement="bottom">
        <span>
          <el-button type="primary" :icon="Refresh" disabled>企微同步</el-button>
        </span>
      </el-tooltip>
    </div>
    <el-empty description="组织架构以企业微信通讯录为源（P2 激活）；当前阶段使用本地部门树与成员管理。" />
  </el-card>
</template>

<script setup>
import { Refresh } from '@element-plus/icons-vue'
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
</style>
