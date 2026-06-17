<template>
  <!-- 立项申请表单（design-system §7-E：按 S/I/O 类型动态字段）。
       字段集为 InitiationFormDTO；不同类型显隐/必填不同。 -->
  <el-form ref="formRef" :model="form" :rules="rules" :label-width="96">
    <el-alert class="hint" type="info" :closable="false" show-icon
      :title="`当前为 ${catLabel} 立项申请`" :description="schema.hint" />

    <el-form-item label="项目目标" prop="objective">
      <el-input v-model="form.objective" type="textarea" :rows="2" placeholder="一句话说明立项要解决的问题/达成的目标" />
    </el-form-item>

    <el-form-item label="项目负责人" prop="leaderId">
      <el-select v-model="form.leaderId" filterable clearable placeholder="选择负责人" class="full">
        <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.jobLevel || '—'}）`" :value="u.id" />
      </el-select>
      <div class="mido-text-secondary">{{ schema.leaderHint }}</div>
    </el-form-item>

    <el-form-item label="预算(元)" prop="budget">
      <el-input-number v-model="form.budget" :min="0" :step="1000" :controls="false" class="full" />
    </el-form-item>

    <el-form-item v-if="schema.valueHypothesis" :label="schema.valueHypothesis.label" prop="valueHypothesis">
      <el-input v-model="form.valueHypothesis" type="textarea" :rows="3"
        :placeholder="schema.valueHypothesis.placeholder" />
    </el-form-item>

    <el-form-item v-if="schema.stakeholderDraft" :label="schema.stakeholderDraft.label" prop="stakeholderDraft">
      <el-input v-model="form.stakeholderDraft" type="textarea" :rows="2"
        :placeholder="schema.stakeholderDraft.placeholder" />
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchMembers } from '@/api/org'
import { PROJECT_CATEGORIES } from '@/api/project'

const props = defineProps({
  category: { type: String, default: 'O' },
})

const formRef = ref()
const users = ref([])
const form = reactive({ objective: '', leaderId: null, budget: null, valueHypothesis: '', stakeholderDraft: '' })

const catLabel = computed(() =>
  PROJECT_CATEGORIES.find((c) => c.value === props.category)?.label || props.category)

// 按 S/I/O 动态字段配置（显隐 + 标签 + 提示）
const SCHEMAS = {
  S: {
    hint: '战略级项目走完整审批链（部门负责人→PMO→分管副总→总经理），负责人需 L3+。',
    leaderHint: 'npss-rule §8：S 类负责人职级门槛 L3 及以上',
    valueHypothesis: { label: '战略价值假设', placeholder: '与公司战略目标的对齐关系、预期价值与衡量口径' },
    stakeholderDraft: { label: '核心干系人', placeholder: '关键决策方/受益方/被影响方草稿（后续在干系人模块细化）' },
  },
  I: {
    hint: '创新级（POC）走轻量审批（部门负责人→PMO），重在价值假设可验证。',
    leaderHint: 'I 类不设职级门槛',
    valueHypothesis: { label: '创新价值假设', placeholder: 'POC 要验证的核心假设与成功判据' },
    stakeholderDraft: null,
  },
  O: {
    hint: '运营级走部门负责人→PMO 审批；负责人需 L2+。可按子类（常规/整改/督办）补充受影响方。',
    leaderHint: 'npss-rule §8：O 类负责人职级门槛 L2 及以上',
    valueHypothesis: null,
    stakeholderDraft: { label: '受影响方', placeholder: '受本次运营/整改/督办影响的部门或岗位' },
  },
}
const schema = computed(() => SCHEMAS[props.category] || SCHEMAS.O)

const rules = computed(() => {
  const r = {
    objective: [{ required: true, message: '请填写项目目标', trigger: 'blur' }],
    leaderId: [{ required: true, message: '请选择项目负责人', trigger: 'change' }],
  }
  if (schema.value.valueHypothesis) {
    r.valueHypothesis = [{ required: true, message: '请填写价值假设', trigger: 'blur' }]
  }
  return r
})

onMounted(async () => {
  users.value = await fetchMembers()
})

async function validate() {
  await formRef.value.validate()
  // 仅提交当前类型可见字段
  const payload = {
    objective: form.objective,
    leaderId: form.leaderId,
    budget: form.budget,
    valueHypothesis: schema.value.valueHypothesis ? form.valueHypothesis : undefined,
    stakeholderDraft: schema.value.stakeholderDraft ? form.stakeholderDraft : undefined,
  }
  return payload
}

defineExpose({ validate })
</script>

<style scoped>
.hint {
  margin-bottom: var(--mido-space-4);
}
.full {
  width: 100%;
}
</style>
