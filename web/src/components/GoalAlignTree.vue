<template>
  <div ref="el" class="gat" v-loading="loading"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { Graph } from '@antv/g6'
import { goalApi, GOAL_TYPES } from '@/api/goal'

const el = ref(null)
const loading = ref(false)
const graph = shallowRef(null)

// 取 design-system token 实际色值（不裸写 hex）
function token(name, fallback) {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

const typeLabel = (t) => GOAL_TYPES.find((x) => x.value === t)?.label || t

async function build() {
  loading.value = true
  try {
    const { goals, alignments } = await goalApi.alignGraph()
    const colors = {
      objective: token('--el-color-primary'),
      kr: token('--el-color-success'),
      project: token('--mido-cat-s'),
      task: token('--el-color-info'),
    }
    const text = token('--el-bg-color')
    const edgeColor = token('--el-border-color')

    const nodes = []
    const edges = []
    goals.forEach((g) => {
      nodes.push({
        id: `g${g.id}`,
        style: { labelText: `${typeLabel(g.type)}·${g.title}`, fill: colors[g.type] || colors.objective, labelFill: text },
      })
      if (g.parentId && g.parentId > 0) {
        edges.push({ source: `g${g.parentId}`, target: `g${g.id}`, style: { stroke: edgeColor } })
      }
    })
    const seen = new Set(nodes.map((n) => n.id))
    alignments.forEach((a) => {
      const tid = `${a.targetType}${a.targetId}`
      if (!seen.has(tid)) {
        seen.add(tid)
        nodes.push({
          id: tid,
          style: { labelText: `${a.targetType === 'project' ? '项目' : '任务'}#${a.targetId}`, fill: colors[a.targetType], labelFill: text },
        })
      }
      // 弱关联：虚线边
      edges.push({ source: `g${a.goalId}`, target: tid, style: { stroke: edgeColor, lineDash: [4, 4] } })
    })

    if (graph.value) {
      graph.value.destroy()
      graph.value = null
    }
    graph.value = new Graph({
      container: el.value,
      autoFit: 'view',
      data: { nodes, edges },
      node: { type: 'rect', style: { size: [140, 32], radius: 4, labelPlacement: 'center' } },
      edge: { type: 'polyline', style: { endArrow: true } },
      layout: { type: 'dagre', rankdir: 'LR', nodesep: 16, ranksep: 48 },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    })
    await graph.value.render()
  } finally {
    loading.value = false
  }
}

defineExpose({ refresh: build })

onMounted(build)
onBeforeUnmount(() => {
  if (graph.value) {
    graph.value.destroy()
    graph.value = null
  }
})
</script>

<style scoped>
.gat {
  width: 100%;
  height: 60vh;
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
}
</style>
