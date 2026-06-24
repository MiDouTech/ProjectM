<template>
  <div class="info">
    <div class="info__bar">
      <h3 class="mido-h2">基本信息</h3>
      <el-button v-if="!editing" link type="primary" :icon="Edit" @click="startEdit">编辑</el-button>
    </div>

    <!-- 只读 -->
    <el-descriptions v-if="!editing" :column="2" border>
      <el-descriptions-item label="项目编码">
        <span class="mido-mono">{{ project.code || '—' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="类型">
        <CategoryBadge :category="project.category" />
      </el-descriptions-item>
      <el-descriptions-item label="子类">{{ project.subCategory || '—' }}</el-descriptions-item>
      <el-descriptions-item label="负责人">{{ userName(project.leaderId) }}</el-descriptions-item>
      <el-descriptions-item label="预算">{{ money(project.budget) }}</el-descriptions-item>
      <el-descriptions-item label="实际成本">{{ money(project.actualCost) }}</el-descriptions-item>
      <el-descriptions-item label="开始">{{ project.startDate || '—' }}</el-descriptions-item>
      <el-descriptions-item label="结束">{{ project.endDate || '—' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ project.description || '—' }}</el-descriptions-item>
    </el-descriptions>

    <!-- 编辑 -->
    <el-form v-else ref="formRef" :model="form" :rules="rules" :label-width="80" class="info__form">
      <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="子类">
        <el-select v-if="project.category === 'O'" v-model="form.subCategory" clearable placeholder="选择子类" class="full">
          <el-option v-for="s in O_SUB_CATEGORIES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input v-else v-model="form.subCategory" placeholder="可选" />
      </el-form-item>
      <el-form-item label="负责人">
        <UserSelect v-model="form.leaderId" placeholder="选择负责人" />
      </el-form-item>
      <el-form-item label="预算">
        <el-input-number v-model="form.budget" :min="0" :step="1000" :controls="false" class="full" />
      </el-form-item>
      <el-form-item label="开始"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item>
      <el-form-item label="结束"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" class="full" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      <el-form-item>
        <el-button @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>

    <!-- 自定义字段（租户自配，配置页 P2-4b 维护） -->
    <CustomFieldsSection entity-type="project" :entity-id="project.id" @changed="$emit('updated')" />

    <!-- 成员 -->
    <div class="info__bar info__bar--mt">
      <h3 class="mido-h2">项目成员</h3>
      <el-button link type="primary" :icon="Plus" @click="memberDialog = true">添加成员</el-button>
    </div>
    <el-table :data="members" stripe>
      <el-table-column label="成员"><template #default="{ row }">{{ userName(row.userId) }}</template></el-table-column>
      <el-table-column prop="projectRole" label="项目角色" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="danger" @click="removeMember(row)">移除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无成员" :image-size="60" /></template>
    </el-table>

    <el-dialog v-model="memberDialog" title="添加成员" width="var(--mido-login-card-width)">
      <el-form :label-width="64">
        <el-form-item label="成员">
          <UserSelect v-model="memberForm.userId" placeholder="选择成员" />
        </el-form-item>
        <el-form-item label="角色"><el-input v-model="memberForm.projectRole" placeholder="如 开发/测试" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addMember">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus } from '@element-plus/icons-vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import UserSelect from '@/components/UserSelect.vue'
import CustomFieldsSection from '@/components/CustomFieldsSection.vue'
import { projectApi, O_SUB_CATEGORIES } from '@/api/project'

const props = defineProps({
  project: { type: Object, required: true },
  members: { type: Array, default: () => [] },
  userName: { type: Function, required: true },
})
const emit = defineEmits(['updated', 'members-changed'])

const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ name: '', subCategory: '', leaderId: null, budget: null, startDate: null, endDate: null, description: '' })
const rules = { name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }] }

const memberDialog = ref(false)
const memberForm = reactive({ userId: null, projectRole: '' })

const money = (v) => (v == null ? '—' : `¥${Number(v).toLocaleString()}`)

function startEdit() {
  const p = props.project
  Object.assign(form, {
    name: p.name, subCategory: p.subCategory, leaderId: p.leaderId, budget: p.budget,
    startDate: p.startDate, endDate: p.endDate, description: p.description,
  })
  editing.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await projectApi.update(props.project.id, { ...form })
    ElMessage.success('已保存')
    editing.value = false
    emit('updated')
  } finally {
    saving.value = false
  }
}
async function addMember() {
  if (!memberForm.userId) return ElMessage.warning('请选择成员')
  saving.value = true
  try {
    await projectApi.addMember(props.project.id, { userId: memberForm.userId, projectRole: memberForm.projectRole })
    ElMessage.success('已添加')
    memberDialog.value = false
    memberForm.userId = null
    memberForm.projectRole = ''
    emit('members-changed')
  } finally {
    saving.value = false
  }
}
async function removeMember(row) {
  await ElMessageBox.confirm(`确认移除成员「${props.userName(row.userId)}」？`, '提示', { type: 'warning' })
  await projectApi.removeMember(props.project.id, row.id)
  ElMessage.success('已移除')
  emit('members-changed')
}
</script>

<style scoped>
.info__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.info__bar--mt {
  margin-top: var(--mido-space-5);
}
.info__form {
  max-width: var(--mido-drawer-width);
}
.full {
  width: 100%;
}
</style>
