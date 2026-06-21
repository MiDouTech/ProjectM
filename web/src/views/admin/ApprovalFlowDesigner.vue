<template>
  <div class="afd">
    <!-- 列表视图：进入先看全部审批流，可新建或打开某条进编辑 -->
    <div v-if="view === 'list'" class="afd-list" v-loading="loading">
      <div class="afd-list__head">
        <h2 class="mido-h2">审批流</h2>
        <el-button type="primary" :icon="Plus" @click="newFlow">新建审批流</el-button>
      </div>
      <el-table v-if="flows.length" :data="flows" class="afd-list__table" @row-click="(row) => openFlow(row.id)">
        <el-table-column label="名称" min-width="220">
          <template #default="{ row }">
            <span class="afd-list__name">{{ row.displayName || row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ bizLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批节点" width="110">
          <template #default="{ row }">{{ nodeCount(row) }} 个</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openFlow(row.id)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else-if="!loading" description="暂无审批流" action-text="新建审批流"
        :action-icon="Plus" @action="newFlow" />
    </div>

    <!-- 编辑视图：返回 + 中文名 + 业务类型 + 保存；节点编辑保留 -->
    <section v-else class="afd-editor" v-loading="loading">
      <div class="afd-editor__bar">
        <el-button :icon="ArrowLeft" @click="backToList">返回</el-button>
        <el-input v-model="form.displayName" placeholder="审批流名称（中文，如 战略级标准流程）" class="afd-editor__name" />
        <el-select v-model="form.bizType" placeholder="业务类型" class="afd-editor__biz">
          <el-option v-for="b in bizTypes" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存</el-button>
      </div>
      <div v-if="form.name" class="afd-editor__code mido-text-secondary">代码键：{{ form.name }}（系统标识，不可改）</div>

      <!-- 流程预览 -->
      <el-steps :active="nodes.length" align-center class="afd-editor__preview">
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
              <el-form-item label="审批人类型">
                <el-select v-model="n.approverType" class="afd__wide">
                  <el-option v-for="t in meta.approverTypes" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="n.approverType === 'USER'" label="指定成员">
                <UserSelect v-model="n.approvers" multiple placeholder="选择审批人" class="afd__wide" />
              </el-form-item>
              <el-form-item v-else-if="n.approverType === 'ROLE'" label="角色">
                <el-select v-model="n.roleIds" multiple placeholder="选择角色" class="afd__wide">
                  <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
                </el-select>
              </el-form-item>
              <el-form-item v-else-if="n.approverType === 'DEPT_HEAD'" label="向上层级">
                <el-input-number v-model="n.deptLevel" :min="1" :max="5" />
                <span class="mido-text-secondary afd__hint">1=发起人部门主管，2=上级部门主管，以此类推</span>
              </el-form-item>
              <el-form-item v-else label="说明">
                <span class="mido-text-secondary">
                  {{ n.approverType === 'DIRECT_LEADER' ? '取发起人所在部门负责人' : '审批人=发起人本人' }}
                </span>
              </el-form-item>
              <el-form-item label="签署方式">
                <el-radio-group v-model="n.mode">
                  <el-radio value="or">或签（任一通过）</el-radio>
                  <el-radio value="and">会签（全部通过）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="职级门槛">
                <el-switch v-model="n.guardOn" />
                <span class="mido-text-secondary afd__hint">开启则按项目类型配置的最低职级门槛校验 Leader</span>
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
import { Plus, Check, Delete, Rank, ArrowLeft } from '@element-plus/icons-vue'
import UserSelect from '@/components/UserSelect.vue'
import EmptyState from '@/components/EmptyState.vue'
import { approvalApi, approvalFlowApi } from '@/api/project'
import { roleApi } from '@/api/org'

const view = ref('list') // 'list' | 'edit'
const loading = ref(false)
const saving = ref(false)
const flows = ref([])
const roles = ref([])
const meta = reactive({ conditionFields: [], conditionOps: [], guards: [], modes: [], approverTypes: [] })
const nodes = ref([])
const form = reactive({ id: null, name: '', displayName: '', bizType: 'project_init', mode: 'fixed' })

let keySeq = 0
// bizType 字典：单一信息源，从后端 /approvals/biz-types 拉取（不再前端硬编码）
const bizTypes = ref([])
const bizLabel = (v) => bizTypes.value.find((b) => b.value === v)?.label || v || '—'

// 列表展示用：解析 definition 统计审批节点数
function nodeCount(row) {
  try {
    return (JSON.parse(row.definition || '{"nodes":[]}').nodes || []).length
  } catch {
    return 0
  }
}

function blankNode() {
  return { _k: `k${keySeq++}`, name: '', approverType: 'USER', approvers: [], roleIds: [], deptLevel: 1,
    mode: 'or', guardOn: false, cc: [], cond: { field: '', op: '', value: '' } }
}

// definition JSON → 可编辑节点模型
function toModel(node) {
  const approverType = node.approverType || 'USER'
  const values = node.approverValues || []
  return {
    _k: `k${keySeq++}`,
    name: node.name || '',
    approverType,
    approvers: node.approvers || [],
    roleIds: approverType === 'ROLE' ? values : [],
    deptLevel: approverType === 'DEPT_HEAD' ? (values[0] || 1) : 1,
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
  let approverValues = null
  if (n.approverType === 'ROLE') approverValues = n.roleIds
  else if (n.approverType === 'DEPT_HEAD') approverValues = [n.deptLevel]
  return {
    key: `n${i + 1}`,
    name: n.name,
    approverType: n.approverType,
    approvers: n.approverType === 'USER' ? n.approvers : [],
    approverValues,
    mode: n.mode,
    guard: n.guardOn ? 'JOB_LEVEL' : null,
    cc: n.cc,
    condition: hasCond ? { field: n.cond.field, op: n.cond.op, value: String(n.cond.value) } : null,
  }
}

async function loadFlows() {
  loading.value = true
  try {
    flows.value = await approvalFlowApi.list()
  } finally {
    loading.value = false
  }
}

function backToList() {
  view.value = 'list'
  loadFlows()
}

// 新建：代码键自动生成（用户只填中文名），避免业务侧 resolveFlowId 误改既有键
function newFlow() {
  form.id = null
  form.name = `FLOW_${Date.now()}`
  form.displayName = ''
  form.bizType = 'project_init'
  form.mode = 'fixed'
  nodes.value = [blankNode()]
  view.value = 'edit'
}

async function openFlow(id) {
  view.value = 'edit'
  loading.value = true
  try {
    const f = await approvalFlowApi.get(id)
    form.id = f.id
    form.name = f.name
    form.displayName = f.displayName || ''
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
  if (!form.displayName.trim()) {
    ElMessage.warning('请填写审批流名称')
    return
  }
  if (!nodes.value.length) {
    ElMessage.warning('至少需要一个审批节点')
    return
  }
  const definition = JSON.stringify({ nodes: nodes.value.map(toNode) })
  const payload = {
    name: form.name,
    displayName: form.displayName.trim(),
    bizType: form.bizType,
    mode: form.mode,
    definition,
  }
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
  roles.value = await roleApi.list()
  try {
    bizTypes.value = await approvalApi.bizTypes() || []
  } catch {
    bizTypes.value = []
  }
  await loadFlows()
})
</script>

<style scoped>
.afd {
  height: 100%;
}

/* 列表视图 */
.afd-list__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.afd-list__table {
  width: 100%;
  cursor: pointer;
}
.afd-list__name {
  font-weight: var(--mido-font-weight-bold);
}

/* 编辑视图 */
.afd-editor__bar {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.afd-editor__name {
  flex: 1;
}
.afd-editor__biz {
  width: var(--mido-admin-nav-width);
}
.afd-editor__code {
  margin-bottom: var(--mido-space-4);
  font-size: var(--mido-font-size-secondary);
}
.afd-editor__preview {
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
