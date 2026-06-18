<template>
  <div :class="{ 'mido-page': !embedded }">
    <div class="sv__bar">
      <div class="sv__bar-left">
        <template v-if="!embedded">
          <el-button :icon="ArrowLeft" link @click="$router.push('/project')">返回项目</el-button>
          <h1 class="mido-h1">{{ project.name || '干系人' }}</h1>
          <CategoryBadge v-if="project.category" :category="project.category" :show-label="false" />
        </template>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">登记干系人</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 干系人列表 + 权重微调 -->
      <el-col :span="14">
        <el-card shadow="never" v-loading="loading">
          <div class="sv__head">
            <h3 class="mido-h2">干系人与 NPSS 权重</h3>
            <el-button link type="primary" @click="showDefault = true">默认权重模板</el-button>
          </div>

          <el-table :data="rows" stripe>
            <el-table-column label="干系人">
              <template #default="{ row }">{{ displayName(row) }}</template>
            </el-table-column>
            <el-table-column label="角色" width="120">
              <template #default="{ row }">
                <span class="sv__role" :class="{ 'sv__role--ben': isBeneficiaryRole(row.role) }">
                  {{ ROLE_LABEL[row.role] || row.role }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="权力" width="70">
              <template #default="{ row }">{{ row.powerLevel ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="利益" width="70">
              <template #default="{ row }">{{ row.interestLevel ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="权重(%)" width="130">
              <template #default="{ row }">
                <el-input-number v-model="weights[row.id]" :min="0" :max="100" :step="5"
                  :controls="false" size="small" class="sv__w" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无干系人，点击右上角登记" /></template>
          </el-table>

          <!-- 权重硬校验提示（npss-rule §4：受益方≥50% 且 总和=100%） -->
          <el-alert v-if="rows.length" class="sv__check" :closable="false" show-icon
            :type="weightValid ? 'success' : 'error'"
            :title="weightValid ? '权重校验通过' : '权重不达标，保存将被后端拒绝'">
            <div class="sv__check-detail">
              <span :class="sumOk ? 'sv__ok' : 'sv__bad'">总和 {{ sum }}% / 100%</span>
              <span :class="benOk ? 'sv__ok' : 'sv__bad'">受益方(发起人+业务方) {{ beneficiarySum }}% / ≥50%</span>
            </div>
          </el-alert>
          <div v-if="rows.length" class="sv__save">
            <el-button type="primary" :loading="saving" @click="saveWeights">保存权重</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 权力利益矩阵 -->
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading">
          <h3 class="mido-h2">权力利益矩阵</h3>
          <PowerInterestMatrix :points="matrix" @select="openDetailById" />
          <div class="sv__legend mido-text-secondary">
            <span><i class="sv__dot sv__dot--ben" />受益方</span>
            <span><i class="sv__dot sv__dot--other" />其他干系人</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 登记/编辑（右抽屉） -->
    <el-drawer v-model="drawer" :title="editing ? '编辑干系人' : '登记干系人'" :size="'var(--mido-drawer-width)'">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="88">
        <el-form-item v-if="!editing" label="干系人类型">
          <el-radio-group v-model="form.internal">
            <el-radio :value="true">内部成员</el-radio>
            <el-radio :value="false">外部</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.internal && !editing" label="成员" prop="userId">
          <el-select v-model="form.userId" filterable placeholder="选择成员" class="full">
            <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="姓名" prop="externalName">
          <el-input v-model="form.externalName" placeholder="外部干系人姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="选择角色" class="full">
            <el-option v-for="r in STAKEHOLDER_ROLES" :key="r.value"
              :label="`${r.label}${r.beneficiary ? '（受益方）' : ''}`" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="权力等级">
          <el-rate v-model="form.powerLevel" :max="5" show-score />
        </el-form-item>
        <el-form-item label="利益等级">
          <el-rate v-model="form.interestLevel" :max="5" show-score />
        </el-form-item>
        <el-form-item label="NPSS权重(%)">
          <el-input-number v-model="form.npssWeight" :min="0" :max="100" :step="5" :controls="false" class="full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 默认权重模板 -->
    <el-dialog v-model="showDefault" title="默认权重模板（npss-rule §6）" width="var(--mido-login-card-width)">
      <p class="mido-text-secondary">按当前项目类型/子类预置，供 PMO 参考微调（实际权重按上方逐人填写）。</p>
      <el-table :data="defaultWeights">
        <el-table-column label="角色">
          <template #default="{ row }">{{ ROLE_LABEL[row.role] || row.role }}</template>
        </el-table-column>
        <el-table-column label="权重(%)" prop="weight" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import PowerInterestMatrix from '@/components/PowerInterestMatrix.vue'
import { stakeholderApi, STAKEHOLDER_ROLES, ROLE_LABEL, isBeneficiaryRole } from '@/api/stakeholder'
import { projectApi } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { userName } from '@/utils/display'

// 内嵌模式：作为「项目工作台」子标签渲染，隐藏自带页头（返回/标题），projectId 由父级透传
const props = defineProps({
  embedded: { type: Boolean, default: false },
  projectId: { type: [Number, String], default: null },
})

const route = useRoute()
// 雪花 ID 为字符串，禁止 Number() 转换（会丢精度），直接透传给后端
const projectId = props.projectId ?? route.params.projectId

const loading = ref(false)
const saving = ref(false)
const project = ref({})
const rows = ref([])
const matrix = ref([])
const users = ref([])
const weights = reactive({})
const defaultWeights = ref([])
const showDefault = ref(false)

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, internal: true, userId: null, externalName: '', role: '', powerLevel: 3, interestLevel: 3, npssWeight: null })
const rules = {
  userId: [{ required: true, message: '请选择成员', trigger: 'change' }],
  externalName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const displayName = (s) => s.externalName || userName(users.value, s.userId)

// 权重实时校验（镜像后端 npss-rule §4）
const round2 = (n) => Math.round(n * 100) / 100
const sum = computed(() => round2(rows.value.reduce((a, r) => a + (Number(weights[r.id]) || 0), 0)))
const beneficiarySum = computed(() =>
  round2(rows.value.filter((r) => isBeneficiaryRole(r.role)).reduce((a, r) => a + (Number(weights[r.id]) || 0), 0)))
const sumOk = computed(() => Math.abs(sum.value - 100) <= 0.01)
const benOk = computed(() => beneficiarySum.value >= 50)
const weightValid = computed(() => sumOk.value && benOk.value)

async function load() {
  loading.value = true
  try {
    const [list, mtx] = await Promise.all([
      stakeholderApi.list(projectId),
      stakeholderApi.matrix(projectId),
    ])
    rows.value = list
    matrix.value = mtx
    Object.keys(weights).forEach((k) => delete weights[k])
    rows.value.forEach((r) => { weights[r.id] = r.npssWeight != null ? Number(r.npssWeight) : 0 })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, internal: true, userId: null, externalName: '', role: '', powerLevel: 3, interestLevel: 3, npssWeight: null })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, internal: row.userId != null, userId: row.userId, externalName: row.externalName,
    role: row.role, powerLevel: row.powerLevel || 3, interestLevel: row.interestLevel || 3, npssWeight: row.npssWeight,
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const base = {
      role: form.role, powerLevel: form.powerLevel, interestLevel: form.interestLevel,
      npssWeight: form.npssWeight, externalName: form.internal ? undefined : form.externalName,
    }
    if (editing.value) {
      await stakeholderApi.update(form.id, base)
    } else {
      await stakeholderApi.create({
        projectId, userId: form.internal ? form.userId : undefined,
        externalName: form.internal ? undefined : form.externalName, ...base,
      })
    }
    ElMessage.success('已保存')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除干系人「${displayName(row)}」？`, '提示', { type: 'warning' })
  await stakeholderApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}
async function saveWeights() {
  saving.value = true
  try {
    await stakeholderApi.saveWeights({
      projectId,
      items: rows.value.map((r) => ({ stakeholderId: r.id, npssWeight: weights[r.id] || 0 })),
    })
    ElMessage.success('权重已保存')
    load()
  } finally {
    saving.value = false
  }
}
function openDetailById(id) {
  const row = rows.value.find((r) => r.id === id)
  if (row) openEdit(row)
}

watch(showDefault, async (v) => {
  if (v && !defaultWeights.value.length) {
    defaultWeights.value = await stakeholderApi.defaultWeights(project.value.category, project.value.subCategory)
  }
})

onMounted(async () => {
  const [proj, members] = await Promise.all([projectApi.get(projectId), fetchMembers()])
  project.value = proj
  users.value = members
  load()
})
</script>

<style scoped>
.sv__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.sv__bar-left {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
}
.sv__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-3);
}
.sv__w {
  width: 100%;
}
/* 角色为分类语义（非状态），用 token 着色的文本区分受益方/其他，不写死 el-tag type */
.sv__role {
  color: var(--el-text-color-regular);
}
.sv__role--ben {
  color: var(--el-color-primary);
  font-weight: var(--mido-font-weight-bold);
}
.sv__check {
  margin-top: var(--mido-space-4);
}
.sv__check-detail {
  display: flex;
  gap: var(--mido-space-5);
  margin-top: var(--mido-space-1);
}
.sv__ok {
  color: var(--el-color-success);
}
.sv__bad {
  color: var(--el-color-danger);
  font-weight: var(--mido-font-weight-bold);
}
.sv__save {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mido-space-3);
}
.sv__legend {
  display: flex;
  gap: var(--mido-space-4);
  justify-content: center;
  margin-top: var(--mido-space-3);
}
.sv__dot {
  display: inline-block;
  width: var(--mido-space-2);
  height: var(--mido-space-2);
  border-radius: 50%;
  margin-right: var(--mido-space-1);
}
.sv__dot--ben {
  background-color: var(--el-color-primary);
}
.sv__dot--other {
  background-color: var(--el-color-info);
}
.full {
  width: 100%;
}
</style>
