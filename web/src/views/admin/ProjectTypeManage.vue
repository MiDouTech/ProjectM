<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">项目类型</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建类型</el-button>
    </div>
    <p class="mido-text-secondary tip">
      项目类型由租户自配（取代固定的 S/I/O）。职级门槛、是否走 NPSS、绑定的审批流均在此设置，立项时按类型生效。
    </p>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="类型" min-width="160">
        <template #default="{ row }">
          <el-tag :type="row.color || 'info'" effect="light" disable-transitions>{{ row.name }}</el-tag>
          <span class="mido-text-secondary code">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="minJobLevel" label="职级门槛" width="100">
        <template #default="{ row }">{{ row.minJobLevel || '不限' }}</template>
      </el-table-column>
      <el-table-column label="走 NPSS" width="90">
        <template #default="{ row }">{{ row.requiresNpss === 0 ? '否' : '是' }}</template>
      </el-table-column>
      <el-table-column label="绑定审批流" min-width="140">
        <template #default="{ row }">{{ flowName(row.defaultFlowId) || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" effect="plain" disable-transitions>
            {{ row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'active'" link type="warning" @click="toggle(row, false)">停用</el-button>
          <el-button v-else link type="success" @click="toggle(row, true)">启用</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无项目类型，点击新建" /></template>
    </el-table>

    <el-drawer v-model="drawer" :title="editing ? '编辑项目类型' : '新建项目类型'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="92">
        <el-form-item label="类型码" prop="code">
          <el-input v-model="form.code" :disabled="editing" placeholder="租户内唯一，如 S / O_NORMAL" />
        </el-form-item>
        <el-form-item label="显示名" prop="name">
          <el-input v-model="form.name" placeholder="如 战略级 / 运营级·常规运营" />
        </el-form-item>
        <el-form-item label="标签颜色">
          <el-select v-model="form.color" placeholder="选择颜色">
            <el-option v-for="c in PROJECT_TYPE_COLORS" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级类型码">
          <el-input v-model="form.parentCode" placeholder="报表汇总用，可空（如 O_* 填 O）" />
        </el-form-item>
        <el-form-item label="职级门槛">
          <el-input v-model="form.minJobLevel" placeholder="如 L3，空=不限" />
          <span class="mido-text-secondary hint">立项 Leader 最低职级，空表示不限</span>
        </el-form-item>
        <el-form-item label="走 NPSS">
          <el-switch v-model="form.requiresNpss" :active-value="1" :inactive-value="0" />
          <span class="mido-text-secondary hint">默认是否走 NPSS 价值验收</span>
        </el-form-item>
        <el-form-item label="强制对齐目标">
          <el-switch v-model="form.requireGoalAlignment" :active-value="1" :inactive-value="0" />
          <span class="mido-text-secondary hint">开启后该类型立项前须已对齐至少一个目标(OKR)</span>
        </el-form-item>
        <el-form-item label="绑定审批流">
          <el-select v-model="form.defaultFlowId" clearable placeholder="选择立项审批流" class="full">
            <el-option v-for="f in flows" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { projectTypeApi, approvalFlowApi, PROJECT_TYPE_COLORS } from '@/api/project'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const flows = ref([])

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({
  id: null, code: '', name: '', color: 'info', parentCode: '',
  minJobLevel: '', requiresNpss: 1, requireGoalAlignment: 0, defaultFlowId: null, sort: 0, description: '',
})
const rules = {
  code: [{ required: true, message: '请输入类型码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
}

function flowName(id) {
  return flows.value.find((f) => f.id === id)?.name
}

async function load() {
  loading.value = true
  try {
    const [types, flowList] = await Promise.all([
      projectTypeApi.list(false),
      approvalFlowApi.list('project_init'),
    ])
    rows.value = types
    flows.value = flowList
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, {
    id: null, code: '', name: '', color: 'info', parentCode: '',
    minJobLevel: '', requiresNpss: 1, defaultFlowId: null, sort: 0, description: '',
  })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, color: row.color || 'info',
    parentCode: row.parentCode || '', minJobLevel: row.minJobLevel || '',
    requiresNpss: row.requiresNpss ?? 1, requireGoalAlignment: row.requireGoalAlignment ?? 0,
    defaultFlowId: row.defaultFlowId, sort: row.sort ?? 0, description: row.description || '',
  })
  drawer.value = true
}

function payload() {
  return {
    code: form.code, name: form.name, parentCode: form.parentCode || null,
    color: form.color, sort: form.sort, minJobLevel: form.minJobLevel || null,
    requiresNpss: form.requiresNpss, requireGoalAlignment: form.requireGoalAlignment,
    defaultFlowId: form.defaultFlowId, description: form.description || null,
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) await projectTypeApi.update(form.id, payload())
    else await projectTypeApi.create(payload())
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, active) {
  await projectTypeApi.setStatus(row.id, active)
  ElMessage.success(active ? '已启用' : '已停用')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.tip {
  margin-bottom: var(--mido-space-4);
}
.code {
  margin-left: var(--mido-space-2);
}
.hint {
  margin-left: var(--mido-space-2);
}
.full {
  width: 100%;
}
</style>
