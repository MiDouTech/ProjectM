<template>
  <div class="afd">
    <!-- 左：流程列表 -->
    <aside class="afd__list">
      <div class="afd__list-head">
        <h3 class="mido-h2">审批流</h3>
        <el-button type="primary" link :icon="Plus" @click="newFlow">新建</el-button>
      </div>
      <el-menu :default-active="String(form.id || 'new')">
        <el-menu-item v-for="f in flows" :key="f.id" :index="String(f.id)" @click="select(f.id)">
          <div class="afd__item">
            <span>{{ f.name }}</span>
            <el-tag size="small" effect="plain">{{ bizLabel(f.bizType) }}</el-tag>
          </div>
        </el-menu-item>
        <el-menu-item v-if="form.id === null" index="new">
          <span class="mido-text-secondary">＊未保存的新流程</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右：编辑器 -->
    <section class="afd__editor" v-loading="loading">
      <div class="afd__editor-head">
        <el-input v-model="form.name" placeholder="流程名称（如 S_STANDARD）" class="afd__name" />
        <el-select v-model="form.bizType" placeholder="业务类型" class="afd__biz">
          <el-option v-for="b in APPROVAL_BIZ_TYPES" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存</el-button>
      </div>

      <!-- 流程预览 -->
      <el-steps :active="nodes.length" align-center class="afd__preview">
        <el-step v-for="(n, i) in nodes" :key="i" :title="n.name || `节点${i + 1}`"
          :description="n.mode === 'and' ? '会签' : '或签'" />
      </el-steps>

      <!-- 节点编辑（拖拽排序） -->
      <draggable v-model="nodes" item-key="_k" handle=".afd__drag" class="afd__nodes">
        <template #item="{ element: n, index }">
          <el-card shadow="never" class="afd__node">
            <div class="afd__node-head">
              <el-icon class="afd__drag"><Rank /></el-icon>
              <span class="afd__node-no">节点 {{ index + 1 }}</span>
              <el-input v-model="n.name" placeholder="节点名（如 部门负责人）" class="afd__node-name" />
              <el-button type="danger" link :icon="Delete" @click="removeNode(index)">删除</el-button>
            </div>
            <el-form label-width="84px" label-position="left" class="afd__node-form">
              <el-form-item label="审批人">
                <UserSelect v-model="n.approvers" multiple placeholder="选择审批人" class="afd__wide" />
              </el-form-item>
              <el-form-item label="签署方式">
                <el-radio-group v-model="n.mode">
                  <el-radio value="or">或签（任一通过）</el-radio>
                  <el-radio value="and">会签（全部通过）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="职级门槛">
                <el-switch v-model="n.guardOn" />
                <span class="mido-text-secondary afd__hint">开启则按项目类型校验 Leader 职级（S→L3+/O→L2+）</span>
              </el-form-item>
              <el-form-item label="知会人">
                <UserSelect v-model="n.cc" multiple placeholder="可选" class="afd__wide" />
              </el-form-item>
              <el-form-item label="路由条件">
                <div class="afd__cond">
                  <el-select v-model="n.cond.field" placeholder="字段" clearable class="afd__cond-f">
                    <el-option v-for="f in meta.conditionFields" :key="f" :label="f" :value="f" />
                  </el-select>
                  <el-select v-model="n.cond.op" placeholder="运算" clearable class="afd__cond-op">
                    <el-option v-for="o in meta.conditionOps" :key="o" :label="o" :value="o" />
                  </el-select>
                  <el-input v-model="n.cond.value" placeholder="值（空=恒启用）" class="afd__cond-v" />
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>
      </draggable>

      <el-button :icon="Plus" class="afd__add" @click="addNode">添加节点</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import draggable from 'vuedraggable'
import { ElMessage } from 'element-plus'
import { Plus, Check, Delete, Rank } from '@element-plus/icons-vue'
import UserSelect from '@/components/UserSelect.vue'
import { approvalFlowApi, APPROVAL_BIZ_TYPES } from '@/api/project'

const loading = ref(false)
const saving = ref(false)
const flows = ref([])
const meta = reactive({ conditionFields: [], conditionOps: [], guards: [], modes: [] })
const nodes = ref([])
const form = reactive({ id: null, name: '', bizType: 'project_init', mode: 'fixed' })

let keySeq = 0
const bizLabel = (v) => APPROVAL_BIZ_TYPES.find((b) => b.value === v)?.label || v || '—'

function blankNode() {
  return { _k: `k${keySeq++}`, name: '', approvers: [], mode: 'or', guardOn: false, cc: [],
    cond: { field: '', op: '', value: '' } }
}

// definition JSON → 可编辑节点模型
function toModel(node) {
  return {
    _k: `k${keySeq++}`,
    name: node.name || '',
    approvers: node.approvers || [],
    mode: node.mode || 'or',
    guardOn: node.guard === 'JOB_LEVEL',
    cc: node.cc || [],
    cond: node.condition
      ? { field: node.condition.field || '', op: node.condition.op || '', value: node.condition.value ?? '' }
      : { field: '', op: '', value: '' },
  }
}

// 可编辑节点模型 → definition 节点
function toNode(n, i) {
  const hasCond = n.cond.field && n.cond.op && n.cond.value !== ''
  return {
    key: `n${i + 1}`,
    name: n.name,
    approvers: n.approvers,
    mode: n.mode,
    guard: n.guardOn ? 'JOB_LEVEL' : null,
    cc: n.cc,
    condition: hasCond ? { field: n.cond.field, op: n.cond.op, value: String(n.cond.value) } : null,
  }
}

async function loadFlows() {
  flows.value = await approvalFlowApi.list()
}

function newFlow() {
  form.id = null
  form.name = ''
  form.bizType = 'project_init'
  nodes.value = [blankNode()]
}

async function select(id) {
  loading.value = true
  try {
    const f = await approvalFlowApi.get(id)
    form.id = f.id
    form.name = f.name
    form.bizType = f.bizType || 'project_init'
    form.mode = f.mode || 'fixed'
    let def = { nodes: [] }
    try {
      def = JSON.parse(f.definition || '{"nodes":[]}')
    } catch {
      def = { nodes: [] }
    }
    nodes.value = (def.nodes || []).map(toModel)
    if (!nodes.value.length) nodes.value = [blankNode()]
  } finally {
    loading.value = false
  }
}

function addNode() {
  nodes.value.push(blankNode())
}
function removeNode(i) {
  nodes.value.splice(i, 1)
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写流程名称')
    return
  }
  if (!nodes.value.length) {
    ElMessage.warning('至少需要一个审批节点')
    return
  }
  const definition = JSON.stringify({ nodes: nodes.value.map(toNode) })
  const payload = { name: form.name.trim(), bizType: form.bizType, mode: form.mode, definition }
  saving.value = true
  try {
    if (form.id) {
      await approvalFlowApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      const id = await approvalFlowApi.create(payload)
      form.id = id
      ElMessage.success('已创建')
    }
    await loadFlows()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const m = await approvalFlowApi.designerMeta()
  Object.assign(meta, m)
  await loadFlows()
  if (flows.value.length) await select(flows.value[0].id)
  else newFlow()
})
</script>

<style scoped>
.afd {
  display: flex;
  gap: var(--mido-space-4);
  height: 100%;
}
.afd__list {
  width: var(--mido-nav-width);
  flex: none;
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  overflow: hidden;
}
.afd__list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--mido-space-3);
}
.afd__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mido-space-2);
  width: 100%;
}
.afd__editor {
  flex: 1;
  min-width: 0;
}
.afd__editor-head {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
}
.afd__name {
  flex: 1;
}
.afd__biz {
  width: var(--mido-admin-nav-width);
}
.afd__preview {
  margin-bottom: var(--mido-space-4);
}
.afd__nodes {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
}
.afd__node-head {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-3);
}
.afd__drag {
  cursor: grab;
  color: var(--el-text-color-secondary);
}
.afd__node-no {
  font-weight: var(--mido-font-weight-bold);
  white-space: nowrap;
}
.afd__node-name {
  flex: 1;
}
.afd__wide {
  width: 100%;
}
.afd__hint {
  margin-left: var(--mido-space-2);
}
.afd__cond {
  display: flex;
  gap: var(--mido-space-2);
  width: 100%;
}
.afd__cond-f,
.afd__cond-op {
  width: var(--mido-admin-nav-width);
}
.afd__cond-v {
  flex: 1;
}
.afd__add {
  margin-top: var(--mido-space-3);
  width: 100%;
}
</style>
