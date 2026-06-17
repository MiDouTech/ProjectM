<template>
  <div ref="el" class="g2c"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { Chart } from '@antv/g2'

const props = defineProps({
  // G2 v5 声明式 spec（type/data/encode/...）
  option: { type: Object, default: () => ({}) },
  height: { type: Number, default: 280 },
})

const el = ref(null)
const chart = shallowRef(null)

function render() {
  if (!el.value) return
  if (!chart.value) {
    chart.value = new Chart({ container: el.value, autoFit: true, height: props.height })
  }
  chart.value.options({ autoFit: true, height: props.height, ...props.option })
  chart.value.render()
}

onMounted(render)
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => {
  chart.value?.destroy()
  chart.value = null
})
</script>

<style scoped>
.g2c {
  width: 100%;
}
</style>
