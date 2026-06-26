<template>
  <span class="tcs-trigger">
    <el-button :icon="Setting" circle plain size="small" title="表头设置" @click="open" />
    <el-dialog v-model="visible" title="表头设置" width="640px" append-to-body @open="syncDraft">
      <div class="tcs">
        <div class="tcs__pane">
          <div class="tcs__h">可选择属性 · {{ allColumns.length }}</div>
          <div class="tcs__list">
            <div v-for="c in allColumns" :key="c.key" class="tcs__opt">
              <el-checkbox :model-value="draftSel.includes(c.key)" :disabled="c.required"
                @change="(v) => toggle(c.key, v)">{{ c.label }}</el-checkbox>
            </div>
          </div>
        </div>
        <div class="tcs__pane">
          <div class="tcs__h">已选择属性 · {{ draftSel.length }}（冻结 {{ draftFrozen.length }}/{{ MAX_FROZEN }}）</div>
          <draggable v-model="draftSel" item-key="self" handle=".tcs__drag" class="tcs__list">
            <template #item="{ element }">
              <div class="tcs__row">
                <el-icon class="tcs__drag"><Rank /></el-icon>
                <span class="tcs__label">{{ labelOf(element) }}</span>
                <el-button link size="small" :type="draftFrozen.includes(element) ? 'primary' : ''"
                  @click="toggleFreeze(element)">{{ draftFrozen.includes(element) ? '已冻结' : '冻结' }}</el-button>
                <el-button link size="small" :disabled="isRequired(element)"
                  @click="toggle(element, false)">移除</el-button>
              </div>
            </template>
          </draggable>
        </div>
      </div>
      <template #footer>
        <el-button link @click="restoreDefault">恢复默认</el-button>
        <span class="tcs__spacer" />
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirm">确定</el-button>
      </template>
    </el-dialog>
  </span>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { tablePrefApi } from '@/api/view'

const props = defineProps({
  // 列表唯一标识（后端 scope=cols:<listKey>）
  listKey: { type: String, required: true },
  // 全部可选列：[{ key, label, required? }]
  allColumns: { type: Array, required: true },
  // 默认展示列（key 顺序）
  defaultColumns: { type: Array, required: true },
})
const emit = defineEmits(['change'])
const MAX_FROZEN = 3

const visible = ref(false)
const saving = ref(false)
const selected = ref([]) // 已应用的展示列（有序）
const frozen = ref([]) // 已应用的冻结列
const draftSel = ref([]) // 对话框草稿
const draftFrozen = ref([])

const labelOf = (key) => props.allColumns.find((c) => c.key === key)?.label || key
const isRequired = (key) => !!props.allColumns.find((c) => c.key === key)?.required

// 规整：仅保留已知列、必选列恒在、冻结为已选子集且≤3，并向父抛出最终结果
function apply(sel, fr) {
  const known = props.allColumns.map((c) => c.key)
  const s = (sel || []).filter((k) => known.includes(k))
  props.allColumns.filter((c) => c.required).forEach((c) => {
    if (!s.includes(c.key)) s.unshift(c.key)
  })
  const f = (fr || []).filter((k) => s.includes(k)).slice(0, MAX_FROZEN)
  selected.value = s
  frozen.value = f
  emit('change', { columns: s, frozen: f })
}

async function load() {
  try {
    const cfg = await tablePrefApi.get(props.listKey)
    if (cfg && Array.isArray(cfg.columns) && cfg.columns.length) apply(cfg.columns, cfg.frozen)
    else apply(props.defaultColumns, [])
  } catch {
    apply(props.defaultColumns, [])
  }
}

function open() {
  visible.value = true
}
function syncDraft() {
  draftSel.value = [...selected.value]
  draftFrozen.value = [...frozen.value]
}
function toggle(key, v) {
  if (v) {
    if (!draftSel.value.includes(key)) draftSel.value.push(key)
  } else {
    if (isRequired(key)) return
    draftSel.value = draftSel.value.filter((k) => k !== key)
    draftFrozen.value = draftFrozen.value.filter((k) => k !== key)
  }
}
function toggleFreeze(key) {
  if (draftFrozen.value.includes(key)) {
    draftFrozen.value = draftFrozen.value.filter((k) => k !== key)
  } else {
    if (draftFrozen.value.length >= MAX_FROZEN) {
      ElMessage.warning(`最多冻结 ${MAX_FROZEN} 列`)
      return
    }
    draftFrozen.value.push(key)
  }
}
function restoreDefault() {
  draftSel.value = [...props.defaultColumns]
  draftFrozen.value = []
}
async function confirm() {
  if (!draftSel.value.length) {
    ElMessage.warning('至少保留一列')
    return
  }
  saving.value = true
  try {
    const cfg = { columns: draftSel.value, frozen: draftFrozen.value }
    await tablePrefApi.save(props.listKey, cfg)
    apply(cfg.columns, cfg.frozen)
    visible.value = false
    ElMessage.success('表头已更新')
  } finally {
    saving.value = false
  }
}

load()
defineExpose({ open })
</script>

<style scoped>
.tcs {
  display: flex;
  gap: var(--mido-space-4);
}
.tcs__pane {
  flex: 1;
  min-width: 0;
}
.tcs__h {
  margin-bottom: var(--mido-space-2);
  color: var(--el-text-color-secondary);
  font-size: var(--mido-font-size-secondary);
}
.tcs__list {
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  padding: var(--mido-space-2);
  height: 320px;
  overflow-y: auto;
}
.tcs__opt {
  padding: var(--mido-space-1) var(--mido-space-1);
}
.tcs__row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  padding: var(--mido-space-1) 0;
}
.tcs__drag {
  cursor: move;
  color: var(--el-text-color-secondary);
}
.tcs__label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tcs__spacer {
  flex: 1;
}
</style>
