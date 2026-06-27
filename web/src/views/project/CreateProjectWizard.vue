<template>
  <el-dialog v-model="visible" title="新建项目" width="calc(var(--mido-drawer-width) * 1.5)" @open="onOpen">
    <el-steps :active="step" finish-status="success" align-center class="wiz__steps">
      <el-step title="选择模板" />
      <el-step title="填写信息" />
      <el-step title="确认创建" />
    </el-steps>

    <!-- Step1：选模板（内置 5 套 + 空白） -->
    <div v-show="step === 0" class="wiz__body">
      <div class="wiz__grid" v-loading="loadingTpl">
        <div class="tpl tpl--blank" :class="{ 'tpl--on': picked === 'blank' }" @click="picked = 'blank'">
          <CategoryBadge category="O" :show-label="false" />
          <div class="tpl__name">空白项目</div>
          <div class="mido-text-secondary">不套用模板，手动填写类型</div>
        </div>
        <div v-for="t in templates" :key="t.id" class="tpl"
          :class="{ 'tpl--on': picked === t.id }" @click="picked = t.id">
          <CategoryBadge :category="t.category" />
          <div class="tpl__name">{{ t.name }}</div>
          <div class="mido-text-secondary tpl__desc">{{ t.description || '内置模板' }}</div>
          <span v-if="t.isBuiltin" class="tpl__builtin mido-text-secondary">内置</span>
        </div>
      </div>
    </div>

    <!-- Step2：填信息 -->
    <div v-show="step === 1" class="wiz__body">
      <!-- 类型/子类为模板驱动的元字段，始终保留 -->
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="88">
        <el-form-item v-if="!usePageConfig" label="项目名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="picked === 'blank'" label="项目类型" prop="category">
          <el-radio-group v-model="form.category">
            <el-radio v-for="c in PROJECT_CATEGORIES" :key="c.value" :value="c.value">
              {{ c.value }} {{ c.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="子类">
          <el-select v-if="effectiveCategory === 'O'" v-model="form.subCategory" clearable placeholder="选择子类" class="full">
            <el-option v-for="s in O_SUB_CATEGORIES" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-input v-else v-model="form.subCategory" placeholder="可选（覆盖模板默认）" />
        </el-form-item>
        <template v-if="!usePageConfig">
          <el-form-item label="负责人" prop="leaderId">
            <UserSelect v-model="form.leaderId" placeholder="选择负责人" />
          </el-form-item>
          <el-form-item label="预算(元)">
            <el-input-number v-model="form.budget" :min="0" :step="1000" :controls="false" class="full" />
          </el-form-item>
          <el-form-item label="周期">
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
              start-placeholder="开始" end-placeholder="结束" class="full" />
          </el-form-item>
        </template>
      </el-form>
      <!-- 标准字段按页面配置渲染（含自定义字段） -->
      <DynamicForm v-if="usePageConfig" ref="dynRef" :fields="pageFields" :model-value="pform" :layout="pageLayout" />
    </div>

    <!-- Step3：确认 -->
    <div v-show="step === 2" class="wiz__body">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模板">{{ pickedName }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
        <el-descriptions-item label="类型"><CategoryBadge :category="effectiveCategory" /></el-descriptions-item>
        <el-descriptions-item label="子类">{{ form.subCategory || '—' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ userName(form.leaderId) }}</el-descriptions-item>
        <el-descriptions-item label="预算">{{ form.budget == null ? '—' : `¥${Number(form.budget).toLocaleString()}` }}</el-descriptions-item>
        <el-descriptions-item label="周期">{{ dateRange ? `${dateRange[0]} ~ ${dateRange[1]}` : '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert class="wiz__note" type="info" :closable="false" show-icon
        title="创建后项目为「草稿」" description="可在项目详情的「状态审批」中发起立项申请，通过后方可进入执行态。" />
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 2" type="primary" :disabled="step === 0 && !picked" @click="next">下一步</el-button>
      <el-button v-else type="primary" :loading="saving" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CategoryBadge from '@/components/CategoryBadge.vue'
import UserSelect from '@/components/UserSelect.vue'
import DynamicForm from '@/components/DynamicForm.vue'
import { projectApi, templateApi, PROJECT_CATEGORIES, O_SUB_CATEGORIES } from '@/api/project'
import { pageConfigApi } from '@/api/view'
import { fieldDefApi, fieldValueApi } from '@/api/field'
import { fetchMembers } from '@/api/org'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'created'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const step = ref(0)
const loadingTpl = ref(false)
const saving = ref(false)
const templates = ref([])
const users = ref([])
const picked = ref(null)
const formRef = ref()
const form = reactive({ name: '', category: 'O', subCategory: '', leaderId: null, budget: null })
const dateRange = ref(null)
const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  leaderId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
}

// 可配置建单表单（项目）：标准字段走 DynamicForm，category/子类保留为向导元字段；无配置回落原向导
const dynRef = ref()
const usePageConfig = ref(false)
const pageFields = ref([])
const pageLayout = ref({ columns: 1 })
const pform = reactive({ name: '', description: '', leaderId: null, budget: null, startDate: null, endDate: null })
const PROJECT_FORM_BUILTIN = {
  name: { label: '项目名称', type: 'text', required: true },
  description: { label: '描述', type: 'text' },
  leaderId: { label: '负责人', type: 'user', required: true },
  budget: { label: '预算(元)', type: 'number' },
  startDate: { label: '开始时间', type: 'date' },
  endDate: { label: '截止时间', type: 'date' },
}
function parseCfOptions(json) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}
async function loadPageConfig() {
  try {
    const [cfg, customDefs] = await Promise.all([
      pageConfigApi.get('project', 'form'),
      fieldDefApi.list('project', true).catch(() => []),
    ])
    const byKey = new Map((customDefs || []).map((d) => [d.fieldKey, d]))
    const fields = (cfg?.fields || []).map((f) => {
      if (f.source === 'builtin' && PROJECT_FORM_BUILTIN[f.fieldKey]) {
        const b = PROJECT_FORM_BUILTIN[f.fieldKey]
        return {
          fieldKey: f.fieldKey, source: 'builtin', label: b.label, type: b.type,
          // 后端必填的内置字段(name/leaderId)恒为必填，配置不可下调，避免提交触发服务端校验错
          required: b.required || (f.required ?? false), readonly: !!f.readonly, width: f.width, group: f.group || '',
        }
      }
      if (f.source === 'custom' && byKey.has(f.fieldKey)) {
        const d = byKey.get(f.fieldKey)
        return {
          fieldKey: f.fieldKey, source: 'custom', fieldId: d.id, label: d.name, type: d.type,
          options: parseCfOptions(d.options), required: f.required ?? d.required === 1, readonly: !!f.readonly,
          width: f.width, group: f.group || '',
        }
      }
      return null
    }).filter(Boolean)
    // 兜底：配置须含后端必填的内置字段(项目名称+负责人)方启用，缺任一则回落原向导，
    // 否则建单将丢失这些输入并在提交时触发服务端 @NotNull 校验错
    const REQUIRED_BUILTINS = ['name', 'leaderId']
    if (fields.length && REQUIRED_BUILTINS.every((k) => fields.some((f) => f.fieldKey === k))) {
      pageFields.value = fields
      pageLayout.value = cfg.layout || { columns: 1 }
      usePageConfig.value = true
    } else {
      usePageConfig.value = false
    }
  } catch {
    usePageConfig.value = false
  }
}

const pickedTpl = computed(() => templates.value.find((t) => t.id === picked.value))
const pickedName = computed(() => (picked.value === 'blank' ? '空白项目' : pickedTpl.value?.name || '—'))
const effectiveCategory = computed(() => (picked.value === 'blank' ? form.category : pickedTpl.value?.category || form.category))
const userName = (id) => users.value.find((u) => String(u.id) === String(id))?.name || (id ? `用户#${id}` : '—')

async function onOpen() {
  step.value = 0
  picked.value = null
  Object.assign(form, { name: '', category: 'O', subCategory: '', leaderId: null, budget: null })
  Object.assign(pform, { name: '', description: '', leaderId: null, budget: null, startDate: null, endDate: null })
  dateRange.value = null
  loadingTpl.value = true
  try {
    templates.value = await templateApi.list()
    users.value = await fetchMembers()
    await loadPageConfig()
    if (usePageConfig.value) {
      pageFields.value.filter((f) => f.source === 'custom').forEach((f) => { pform[f.fieldKey] = null })
    }
  } finally {
    loadingTpl.value = false
  }
}

async function next() {
  if (step.value === 1) {
    await formRef.value.validate()
    if (usePageConfig.value) {
      await dynRef.value.validate()
      // 同步标准字段回 form/dateRange，使确认步与提交逻辑维持不变
      form.name = pform.name
      form.leaderId = pform.leaderId
      form.budget = pform.budget
      dateRange.value = [pform.startDate || null, pform.endDate || null]
    }
  }
  step.value++
}

async function submit() {
  saving.value = true
  try {
    const [startDate, endDate] = dateRange.value || [null, null]
    let projectId
    if (picked.value === 'blank') {
      projectId = await projectApi.create({
        name: form.name, category: form.category, subCategory: form.subCategory || undefined,
        leaderId: form.leaderId, budget: form.budget, startDate, endDate,
      })
    } else {
      const vo = await projectApi.createFromTemplate({
        templateId: picked.value, name: form.name, subCategory: form.subCategory || undefined,
        leaderId: form.leaderId, budget: form.budget, startDate, endDate,
      })
      projectId = vo.projectId
    }
    await saveCustomFieldValues(projectId)
    ElMessage.success('项目已创建（草稿）')
    visible.value = false
    emit('created', projectId)
  } finally {
    saving.value = false
  }
}

async function saveCustomFieldValues(projectId) {
  if (!usePageConfig.value || !projectId) return
  const values = pageFields.value
    .filter((f) => f.source === 'custom' && f.fieldId)
    .map((f) => {
      const v = pform[f.fieldKey]
      return { fieldId: f.fieldId, value: Array.isArray(v) ? JSON.stringify(v) : v }
    })
    .filter((v) => v.value != null && v.value !== '')
  if (!values.length) return
  try {
    await fieldValueApi.save({ entityType: 'project', entityId: projectId, values })
  } catch {
    ElMessage.warning('项目已创建，但部分自定义字段值未保存，可在详情补填')
  }
}
</script>

<style scoped>
.wiz__steps {
  margin-bottom: var(--mido-space-5);
}
.wiz__body {
  min-height: calc(var(--mido-drawer-width) * 0.6);
}
.wiz__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--mido-space-3);
}
.tpl {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
  padding: var(--mido-space-4);
  border: var(--mido-border-width) solid var(--el-border-color);
  border-radius: var(--mido-radius-md);
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
}
.tpl:hover {
  box-shadow: var(--mido-shadow-card);
}
.tpl--on {
  border-color: var(--el-color-primary);
  box-shadow: var(--mido-shadow-card);
}
.tpl__name {
  font-weight: var(--mido-font-weight-bold);
}
/* 中性装饰标签（非状态），不写死 el-tag type */
.tpl__builtin {
  align-self: flex-start;
  padding: 0 var(--mido-space-2);
  border: var(--mido-border-width) solid var(--el-border-color);
  border-radius: var(--mido-radius-sm);
}
.tpl__desc {
  flex: 1;
}
.wiz__note {
  margin-top: var(--mido-space-4);
}
.full {
  width: 100%;
}
</style>
