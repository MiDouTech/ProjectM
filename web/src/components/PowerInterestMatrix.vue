<template>
  <!-- 权力利益矩阵四象限（design-system §5.2 自研 SVG）。
       纵轴=权力(下低上高)，横轴=利益(左低右高)；象限分类以后端 quadrant 为准。 -->
  <div class="pim">
    <svg viewBox="0 0 100 100" class="pim__svg" preserveAspectRatio="xMidYMid meet">
      <!-- 象限底色 -->
      <rect x="10" y="10" width="40" height="40" class="pim__q pim__q--satisfy" />
      <rect x="50" y="10" width="40" height="40" class="pim__q pim__q--manage" />
      <rect x="10" y="50" width="40" height="40" class="pim__q pim__q--monitor" />
      <rect x="50" y="50" width="40" height="40" class="pim__q pim__q--inform" />
      <!-- 象限名（font-size 用 SVG 用户单位，非 CSS px） -->
      <text x="30" y="16" font-size="3.2" class="pim__qlabel">令其满意</text>
      <text x="70" y="16" font-size="3.2" class="pim__qlabel">重点管理</text>
      <text x="30" y="88" font-size="3.2" class="pim__qlabel">监督</text>
      <text x="70" y="88" font-size="3.2" class="pim__qlabel">随时告知</text>
      <!-- 坐标轴 -->
      <line x1="10" y1="50" x2="90" y2="50" class="pim__axis" />
      <line x1="50" y1="10" x2="50" y2="90" class="pim__axis" />
      <!-- 点 -->
      <g v-for="p in plotted" :key="p.stakeholderId" class="pim__pt" @click="$emit('select', p.stakeholderId)">
        <circle :cx="p.x" :cy="p.y" r="2.4" :class="p.beneficiary ? 'pim__dot--ben' : 'pim__dot'" />
        <text :x="p.x" :y="p.y - 3.5" font-size="3" class="pim__name">{{ p.name }}</text>
      </g>
    </svg>
    <div class="pim__axis-label pim__axis-label--y mido-text-secondary">权力 →</div>
    <div class="pim__axis-label pim__axis-label--x mido-text-secondary">利益 →</div>
    <el-empty v-if="!points.length" description="暂无干系人数据" :image-size="60" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { isBeneficiaryRole } from '@/api/stakeholder'

const props = defineProps({
  // MatrixPointVO[]: { stakeholderId, name, role, powerLevel, interestLevel, quadrant }
  points: { type: Array, default: () => [] },
})
defineEmits(['select'])

// 值 1..5 映射到绘图区 10..90；权力轴上高下低（y 反向）
const map = (v) => 10 + ((Number(v) || 1) - 1) / 4 * 80
const plotted = computed(() =>
  props.points.map((p) => ({
    ...p,
    x: map(p.interestLevel),
    y: 100 - map(p.powerLevel),
    beneficiary: isBeneficiaryRole(p.role),
  })))
</script>

<style scoped>
.pim {
  position: relative;
}
.pim__svg {
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
}
.pim__q {
  opacity: .5;
}
.pim__q--manage {
  fill: var(--el-color-primary-light-9);
}
.pim__q--satisfy {
  fill: var(--el-color-warning-light-9, var(--el-fill-color-light));
}
.pim__q--inform {
  fill: var(--el-color-success-light-9, var(--el-fill-color-light));
}
.pim__q--monitor {
  fill: var(--el-fill-color-light);
}
.pim__qlabel {
  fill: var(--el-text-color-secondary);
  text-anchor: middle;
}
.pim__axis {
  stroke: var(--el-border-color);
  stroke-width: .4;
  stroke-dasharray: 1.5 1.5;
}
.pim__pt {
  cursor: pointer;
}
.pim__dot {
  fill: var(--el-color-info);
}
.pim__dot--ben {
  fill: var(--el-color-primary);
}
.pim__name {
  fill: var(--el-text-color-primary);
  text-anchor: middle;
}
.pim__axis-label {
  position: absolute;
}
.pim__axis-label--x {
  right: var(--mido-space-2);
  bottom: var(--mido-space-1);
}
.pim__axis-label--y {
  left: var(--mido-space-1);
  top: var(--mido-space-1);
}
</style>
