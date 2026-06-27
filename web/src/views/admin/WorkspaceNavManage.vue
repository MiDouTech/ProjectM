<template>
  <div class="mido-page wn">
    <div class="wn__bar">
      <div class="wn__bar-actions">
        <el-select v-model="module" class="wn__module" @change="load">
          <el-option v-for="m in MODULES" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <el-button @click="restoreDefault">恢复默认</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>
    <p class="mido-text-secondary">
      配置「{{ moduleLabel }}」模块顶部菜单的<b>顺序、名称与显隐</b>；把某项「归属」到另一项，即成为它的二级菜单。改完点右上「保存」生效。
    </p>

    <el-card shadow="never" v-loading="loading" class="wn__card">
      <!-- 列头：让每个控件含义自解释，避免交互藏太深 -->
      <div v-if="rows.length" class="wn__head mido-text-secondary">
        <span class="wn__h-drag" title="拖拽排序">⠿</span>
        <span class="wn__h-switch">显示</span>
        <span class="wn__h-comp">菜单项</span>
        <span class="wn__h-name">自定义名称</span>
        <span class="wn__h-parent">归属</span>
      </div>
      <draggable v-model="rows" item-key="componentCode" handle=".wn__drag" class="wn__list">
        <template #item="{ element }">
          <div class="wn__row">
            <el-icon class="wn__drag" title="拖拽排序"><Rank /></el-icon>
            <el-switch v-model="element.enabled" />
            <div class="wn__comp">
              <span>{{ element.defaultName }}</span>
              <span class="wn__code mido-mono mido-text-secondary">{{ element.componentCode }}</span>
            </div>
            <el-input v-model="element.displayName" :placeholder="`默认：${element.defaultName}`" class="wn__name" />
            <el-select v-model="element.parentCode" clearable placeholder="作为一级菜单" class="wn__parent">
              <el-option v-for="p in parentOptions(element.componentCode)" :key="p.componentCode"
                :label="p.displayName || p.defaultName" :value="p.componentCode" />
            </el-select>
          </div>
        </template>
      </draggable>
      <el-empty v-if="!rows.length" description="该模块暂无可配置菜单项" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { workspaceNavApi } from '@/api/view'

const MODULES = [
  { value: 'project', label: '项目' },
  { value: 'goal', label: '目标' },
  { value: 'approval', label: '审批' },
  { value: 'report', label: '报表' },
  { value: 'doc', label: '文档' },
  { value: 'calendar', label: '日历' },
  { value: 'briefing', label: '简报' },
]

const module = ref('project')
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const moduleLabel = computed(() => MODULES.find((m) => m.value === module.value)?.label || module.value)

// 以 catalog 为全集，叠加已保存编排（顺序/改名/显隐）；未编排项默认启用、置于末尾
function merge(catalog, config) {
  const byCode = new Map(catalog.map((c) => [c.code, c]))
  const out = []
  const seen = new Set()
  ;(config || []).forEach((cfg) => {
    const def = byCode.get(cfg.componentCode)
    if (!def) return
    out.push({ componentCode: def.code, defaultName: def.name, displayName: cfg.displayName || '', parentCode: cfg.parentCode || '', enabled: cfg.enabled !== false })
    seen.add(def.code)
  })
  catalog.forEach((def) => {
    if (seen.has(def.code)) return
    out.push({ componentCode: def.code, defaultName: def.name, displayName: '', parentCode: '', enabled: true })
  })
  return out
}

// 可作父级的项：自身为二级（无父级）且非自己（保持一级嵌套）
function parentOptions(selfCode) {
  return rows.value.filter((r) => r.componentCode !== selfCode && !r.parentCode)
}

async function load() {
  loading.value = true
  try {
    const [catalog, config] = await Promise.all([
      workspaceNavApi.catalog(module.value),
      workspaceNavApi.rawConfig(module.value),
    ])
    rows.value = merge(catalog || [], config || [])
  } finally {
    loading.value = false
  }
}

function restoreDefault() {
  workspaceNavApi.catalog(module.value).then((catalog) => {
    rows.value = merge(catalog || [], [])
    ElMessage.info('已重置为默认（保存后生效）')
  })
}

async function save() {
  saving.value = true
  try {
    const items = rows.value.map((r) => ({
      componentCode: r.componentCode,
      parentCode: r.parentCode || null,
      displayName: r.displayName?.trim() || null,
      enabled: r.enabled,
    }))
    await workspaceNavApi.saveNav(module.value, items)
    ElMessage.success('导航已保存')
  } finally {
    saving.value = false
  }
}

load()
</script>

<style scoped>
.wn__bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.wn__bar-actions {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.wn__module {
  width: 140px;
}
.wn__card {
  margin-top: var(--mido-space-3);
}
.wn__head {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  padding-bottom: var(--mido-space-2);
  font-size: var(--mido-font-size-caption);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
}
.wn__h-drag {
  width: var(--mido-space-4);
  text-align: center;
}
.wn__h-switch {
  width: 40px;
  text-align: center;
}
.wn__h-comp {
  width: 180px;
}
.wn__h-name {
  flex: 1;
}
.wn__h-parent {
  width: 180px;
}
.wn__list {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-1);
}
.wn__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  padding: var(--mido-space-2) 0;
  border-bottom: var(--mido-border-width) solid var(--el-border-color-lighter);
}
.wn__drag {
  cursor: move;
  color: var(--el-text-color-secondary);
  width: var(--mido-space-4);
}
.wn__comp {
  display: flex;
  flex-direction: column;
  width: 180px;
  min-width: 0;
}
.wn__code {
  font-size: var(--mido-font-size-caption);
}
.wn__name {
  flex: 1;
}
.wn__parent {
  width: 180px;
}
</style>
