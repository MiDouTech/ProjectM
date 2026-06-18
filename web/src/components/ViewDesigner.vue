<template>
  <el-dialog :model-value="modelValue" title="视图设计器" width="var(--mido-login-card-width)"
    @update:model-value="$emit('update:modelValue', $event)" @open="reset">
    <!-- 视图元信息 -->
    <el-form :model="form" :label-width="64" class="vd__meta">
      <el-form-item label="视图名"><el-input v-model="form.name" placeholder="如：我的进行中" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type">
          <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="范围">
        <el-radio-group v-model="form.scope">
          <el-radio value="personal">我的视图</el-radio>
          <el-radio value="project">项目视图</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="展示列">
        <el-select v-model="form.columns" multiple collapse-tags placeholder="默认全部">
          <el-option v-for="c in VIEW_COLUMNS" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-steps :active="step" finish-status="success" align-center class="vd__steps">
      <el-step title="分组" /><el-step title="排序" /><el-step title="层级" /><el-step title="查询条件" />
    </el-steps>

    <!-- 01 分组 -->
    <div v-show="step === 0" class="vd__pane">
      <el-form :label-width="64">
        <el-form-item label="分组方式">
          <el-select v-model="form.groupBy">
            <el-option v-for="g in VIEW_GROUP_FIELDS" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <!-- 02 排序 -->
    <div v-show="step === 1" class="vd__pane">
      <div v-for="(s, i) in form.sort" :key="i" class="vd__row">
        <el-select v-model="s.field" placeholder="字段" class="vd__f">
          <el-option v-for="f in VIEW_SORT_FIELDS" :key="f.value" :label="f.label" :value="f.value" />
        </el-select>
        <el-select v-model="s.dir" class="vd__d">
          <el-option label="升序" value="asc" /><el-option label="降序" value="desc" />
        </el-select>
        <el-button link type="danger" :icon="Delete" aria-label="删除排序条件" @click="form.sort.splice(i, 1)" />
      </div>
      <el-button link type="primary" :icon="Plus" @click="form.sort.push({ field: '', dir: 'asc' })">添加排序</el-button>
    </div>

    <!-- 03 层级 -->
    <div v-show="step === 2" class="vd__pane">
      <el-form :label-width="80">
        <el-form-item label="展开层级">
          <el-slider v-model="form.expandLevel" :min="1" :max="5" :marks="{ 1: '1', 3: '3', 5: '5' }" show-stops />
        </el-form-item>
      </el-form>
    </div>

    <!-- 04 查询条件 -->
    <div v-show="step === 3" class="vd__pane">
      <el-radio-group v-model="form.logic" class="vd__logic">
        <el-radio value="and">满足全部(且)</el-radio>
        <el-radio value="or">满足任一(或)</el-radio>
      </el-radio-group>
      <div v-for="(c, i) in form.conditions" :key="i" class="vd__row">
        <el-select v-model="c.field" placeholder="字段" class="vd__f">
          <el-option v-for="f in VIEW_FILTER_FIELDS" :key="f.value" :label="f.label" :value="f.value" />
        </el-select>
        <el-select v-model="c.op" placeholder="算子" class="vd__o">
          <el-option v-for="o in VIEW_OPS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input v-if="!['isNull', 'notNull'].includes(c.op)" v-model="c.value" placeholder="值" class="vd__v" />
        <el-button link type="danger" :icon="Delete" aria-label="删除筛选条件" @click="form.conditions.splice(i, 1)" />
      </div>
      <el-button link type="primary" :icon="Plus"
        @click="form.conditions.push({ field: '', op: 'eq', value: '' })">添加条件</el-button>
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="step++">下一步</el-button>
      <el-button v-else type="primary" :loading="saving" @click="save">保存视图</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  viewApi, VIEW_GROUP_FIELDS, VIEW_SORT_FIELDS, VIEW_FILTER_FIELDS, VIEW_OPS, VIEW_COLUMNS,
} from '@/api/view'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const TYPES = [
  { value: 'list', label: '列表' },
  { value: 'table', label: '表格' },
  { value: 'kanban', label: '看板' },
  { value: 'gantt', label: '甘特' },
  { value: 'calendar', label: '日历' },
]

const step = ref(0)
const saving = ref(false)
const form = reactive({
  name: '', type: 'list', scope: 'personal', columns: [],
  groupBy: '', sort: [], expandLevel: 1, logic: 'and', conditions: [],
})

function reset() {
  step.value = 0
  Object.assign(form, {
    name: '', type: 'list', scope: 'personal', columns: [],
    groupBy: '', sort: [], expandLevel: 1, logic: 'and', conditions: [],
  })
}

function buildConfig() {
  const conds = form.conditions.filter((c) => c.field && c.op).map((c) => ({
    field: c.field,
    op: c.op,
    value: c.op === 'in'
      ? String(c.value || '').split(',').map((s) => s.trim()).filter(Boolean)
      : (c.op === 'isNull' || c.op === 'notNull') ? null : c.value,
  }))
  return {
    groupBy: form.groupBy || null,
    sort: form.sort.filter((s) => s.field).map((s) => ({ field: s.field, dir: s.dir })),
    expandLevel: form.expandLevel,
    filters: conds.length ? { logic: form.logic, conditions: conds } : null,
    columns: form.columns,
  }
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写视图名')
    return
  }
  saving.value = true
  try {
    await viewApi.create({
      name: form.name,
      scope: form.scope,
      type: form.type,
      projectId: form.scope === 'project' ? props.projectId : null,
      config: buildConfig(),
    })
    ElMessage.success('视图已保存')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.vd__steps {
  margin: var(--mido-space-3) 0 var(--mido-space-4);
}
.vd__pane {
  min-height: var(--mido-admin-nav-width);
}
.vd__row {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
  align-items: center;
}
.vd__f { flex: 2; }
.vd__o, .vd__d { flex: 1; }
.vd__v { flex: 2; }
.vd__logic {
  margin-bottom: var(--mido-space-3);
}
</style>
