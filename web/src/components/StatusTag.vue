<template>
  <el-tag :type="tagType" disable-transitions>{{ status }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

/**
 * StatusTag —— 全局唯一状态着色入口（design-system §1.5 + §5.3）。
 * 业务页面禁止自行写 el-tag type，一律用本组件。
 */
const props = defineProps({
  status: { type: String, default: '' },
})

// design-system §1.5 状态映射表（业务状态 → Element Plus tag type）。
// 含 data-model.md 生命周期状态，按 §1.5 五个语义桶归类；未知状态回落 info。
const STATUS_TYPE = {
  // info：未开始 / 已注册 / 中性
  未开始: 'info', 已注册: 'info', 草稿: 'info', 已归档: 'info',
  // primary：进行中 / 价值验收中
  进行中: 'primary', 价值验收中: 'primary', 审批中: 'primary',
  // warning：有风险 / 临期 / 结果验收中
  有风险: 'warning', 临期: 'warning', 结果验收中: 'warning', 结果验收: 'warning',
  // danger：逾期 / 阻塞 / 失败
  逾期: 'danger', 阻塞: 'danger', 失败: 'danger',
  // success：已完成 / 已结案 / 成功 / 已评价
  已完成: 'success', 已结案: 'success', 成功: 'success', 已评价: 'success', 已验收: 'success',
}

const tagType = computed(() => STATUS_TYPE[props.status] || 'info')
</script>
