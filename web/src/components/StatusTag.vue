<template>
  <el-tag :type="tagType" disable-transitions>{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

/**
 * StatusTag —— 全局唯一状态着色入口（design-system §1.5 + §5.3）。
 * 业务页面禁止自行写 el-tag type，一律用本组件。
 */
const props = defineProps({
  status: { type: String, default: '' },
  // 可选展示文案覆盖：用于 active 等同码在不同上下文语义不同的场景
  // （租户 active=正式，套餐/账号 active=启用）。不传则用内置映射。
  label: { type: String, default: '' },
})

// design-system §1.5 状态映射表（业务状态 → Element Plus tag type）。
// 含 data-model.md 生命周期状态，按 §1.5 五个语义桶归类；未知状态回落 info。
const STATUS_TYPE = {
  // info：未开始 / 已注册 / 中性
  未开始: 'info', 已注册: 'info', 草稿: 'info', 已归档: 'info',
  // primary：进行中 / 价值验收中
  进行中: 'primary', 价值验收中: 'primary', 审批中: 'primary',
  // warning：有风险 / 临期 / 结果验收中 / NPSS 混合
  有风险: 'warning', 临期: 'warning', 结果验收中: 'warning', 结果验收: 'warning', 混合: 'warning',
  // danger：逾期 / 阻塞 / 失败（NPSS）
  失败: 'danger',
  // danger：逾期 / 阻塞 / 失败
  逾期: 'danger', 阻塞: 'danger', 失败: 'danger',
  // success：已完成 / 已结案 / 成功 / 已评价 / PMO 达标
  已完成: 'success', 已结案: 'success', 成功: 'success', 已评价: 'success', 已验收: 'success', 达标: 'success',
  未达标: 'warning',
  // 账号启用/停用（§4 派生，待回写 design-system §1.5 登记）
  active: 'success', 启用: 'success',
  disabled: 'info', 停用: 'info',
  // 费用状态（pm_cost.status，待回写 design-system §1.5 登记）
  未发生: 'info', 已发生: 'success', 被退回: 'danger',
  // 项目健康度（报表，待回写 design-system §1.5 登记）
  健康: 'success', 关注: 'warning', 风险: 'danger',
  // 变更单状态（pm_change_request，待回写 design-system §1.5 登记）
  已生效: 'success', 已驳回: 'danger', 已撤回: 'info',
  // 平台运营后台：租户生命周期状态（英文码，待回写 design-system §1.5 登记）
  trial: 'info', suspended: 'warning', expired: 'danger', closed: 'info',
  // 平台收入台账：类型（payment 收款/refund 退款，待回写 design-system §1.5 登记）
  payment: 'success', refund: 'danger',
  // 平台公告：级别 info/warning + 状态 draft/published（待回写 design-system §1.5 登记）
  info: 'info', warning: 'warning', draft: 'info', published: 'success',
  // 平台运营后台：租户数据导出任务状态（待回写 design-system §1.5 登记）
  pending: 'info', processing: 'warning', done: 'success', failed: 'danger',
  // 平台运营后台：租户注销后物理清除状态（待回写 design-system §1.5 登记）
  purged: 'info',
}

// 英文状态码 → 中文展示文案。命中则显示中文，否则原样回显 status
//（保持现有中文 key 向后兼容）。active 默认「启用」（套餐/账号），
// 租户「正式」由调用方经 label 覆盖。
const STATUS_LABEL = {
  trial: '试用', active: '启用', suspended: '停用', expired: '已过期',
  closed: '已注销', disabled: '停用',
  // 收入台账类型
  payment: '收款', refund: '退款',
  // 公告级别 / 状态
  info: '通知', warning: '警告', draft: '草稿', published: '已发布',
  // 租户数据导出任务状态
  pending: '待处理', processing: '处理中', done: '已完成', failed: '失败',
  // 租户注销后物理清除状态
  purged: '已清除',
}

const tagType = computed(() => STATUS_TYPE[props.status] || 'info')
const label = computed(() => props.label || STATUS_LABEL[props.status] || props.status)
</script>
