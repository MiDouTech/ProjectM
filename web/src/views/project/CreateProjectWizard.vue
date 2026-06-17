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
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="88">
        <el-form-item label="项目名称" prop="name"><el-input v-model="form.name" /></el-form-item>
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
        <el-form-item label="负责人">
          <el-select v-model="form.leaderId" filterable clearable placeholder="选择负责人" class="full">
            <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算(元)">
          <el-input-number v-model="form.budget" :min="0" :step="1000" :controls="false" class="full" />
        </el-form-item>
        <el-form-item label="周期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始" end-placeholder="结束" class="full" />
        </el-form-item>
      </el-form>
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
import { projectApi, templateApi, PROJECT_CATEGORIES, O_SUB_CATEGORIES } from '@/api/project'
import { userApi } from '@/api/org'

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
}

const pickedTpl = computed(() => templates.value.find((t) => t.id === picked.value))
const pickedName = computed(() => (picked.value === 'blank' ? '空白项目' : pickedTpl.value?.name || '—'))
const effectiveCategory = computed(() => (picked.value === 'blank' ? form.category : pickedTpl.value?.category || form.category))
const userName = (id) => users.value.find((u) => u.id === id)?.name || (id ? `用户#${id}` : '—')

async function onOpen() {
  step.value = 0
  picked.value = null
  Object.assign(form, { name: '', category: 'O', subCategory: '', leaderId: null, budget: null })
  dateRange.value = null
  loadingTpl.value = true
  try {
    templates.value = await templateApi.list()
    const res = await userApi.query({ page: 1, size: 200 })
    users.value = res.list || []
  } finally {
    loadingTpl.value = false
  }
}

async function next() {
  if (step.value === 1) {
    await formRef.value.validate()
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
    ElMessage.success('项目已创建（草稿）')
    visible.value = false
    emit('created', projectId)
  } finally {
    saving.value = false
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
