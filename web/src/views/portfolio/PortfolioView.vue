<template>
  <div class="pf-page">
    <ProjectSubNav active="portfolios" />
    <div class="pf">
    <!-- 左：项目集列表 -->
    <el-card shadow="never" class="pf__side">
      <div class="bar">
        <h2 class="mido-h2">项目集</h2>
        <el-button type="primary" :icon="Plus" size="small" @click="openCreate">新建</el-button>
      </div>
      <el-menu :default-active="String(currentId)" @select="select">
        <el-menu-item v-for="p in portfolios" :key="p.id" :index="String(p.id)">
          <span class="pf__name">{{ p.name }}</span>
          <span class="pf__count">{{ p.projectCount }}</span>
        </el-menu-item>
      </el-menu>
      <el-empty v-if="!portfolios.length" description="暂无项目集" :image-size="60" />
    </el-card>

    <!-- 右：总览 -->
    <el-card v-loading="loading" shadow="never" class="pf__main">
      <template v-if="overview">
        <div class="bar">
          <div>
            <h2 class="mido-h2">{{ overview.item.name }}</h2>
            <p class="mido-text-secondary">{{ overview.item.description || '—' }}</p>
          </div>
          <div class="bar__right">
            <el-button :icon="Plus" @click="openAddProjects">添加项目</el-button>
            <el-button :icon="Edit" @click="openEdit(overview.item)">编辑</el-button>
            <el-button type="danger" :icon="Delete" @click="removePortfolio(overview.item)">删除</el-button>
          </div>
        </div>

        <!-- 状态汇总 -->
        <div class="pf__stats">
          <StatusTag v-for="(cnt, st) in overview.statusCount" :key="st" :status="st" :label="`${st}：${cnt}`" />
          <span class="mido-text-secondary">可见项目共 {{ overview.projects.length }} 个（按你的数据范围过滤）</span>
        </div>

        <el-table :data="overview.projects" stripe>
          <el-table-column prop="name" label="项目" min-width="180" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="负责人" width="120">
            <template #default="{ row }">{{ userName(row.leaderId) }}</template>
          </el-table-column>
          <el-table-column prop="startDate" label="开始" width="120" />
          <el-table-column prop="endDate" label="结束" width="120" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeProject(row)">移出</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="该项目集暂无可见项目" :image-size="60" /></template>
        </el-table>
      </template>
      <el-empty v-else description="请选择左侧项目集查看总览" />
    </el-card>

    <!-- 新建/编辑项目集 -->
    <el-drawer v-model="drawer" :title="editing ? '编辑项目集' : '新建项目集'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="创建人"><UserSelect v-model="form.ownerId" placeholder="默认为当前用户" /></el-form-item>
        <el-form-item label="成员">
          <UserSelect v-model="form.memberIds" multiple placeholder="添加成员（创建人默认在内）" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 添加项目（仅可加入创建人负责∪参与的项目） -->
    <el-dialog v-model="addDialog" title="添加项目" width="var(--mido-login-card-width)">
      <el-select v-model="selectedProjectIds" multiple filterable placeholder="选择项目" class="full">
        <el-option v-for="p in allProjects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <p v-if="!allProjects.length" class="mido-text-secondary">创建人暂无可加入的项目（负责或参与的项目）。</p>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmAddProjects">添加</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import UserSelect from '@/components/UserSelect.vue'
import ProjectSubNav from '@/views/project/ProjectSubNav.vue'
import { portfolioApi } from '@/api/project'
import { fetchMembers } from '@/api/org'
import { userName as nameOf } from '@/utils/display'

const loading = ref(false)
const saving = ref(false)
const portfolios = ref([])
const currentId = ref(null)
const overview = ref(null)
const members = ref([])
const userName = (id) => nameOf(members.value, id)

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', ownerId: null, description: '', memberIds: [] })
const rules = { name: [{ required: true, message: '请输入项目集名称', trigger: 'blur' }] }

const addDialog = ref(false)
const allProjects = ref([])
const selectedProjectIds = ref([])

async function loadList() {
  portfolios.value = await portfolioApi.list()
}
async function select(id) {
  // 雪花 ID 为 19 位，禁止 Number() 转换（会丢精度→后端「项目集不存在」），保持字符串透传
  currentId.value = String(id)
  loading.value = true
  try {
    overview.value = await portfolioApi.overview(currentId.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, name: '', ownerId: null, description: '', memberIds: [] })
  drawer.value = true
}
async function openEdit(item) {
  editing.value = true
  Object.assign(form, {
    id: item.id, name: item.name, ownerId: item.ownerId, description: item.description, memberIds: [],
  })
  drawer.value = true
  form.memberIds = await portfolioApi.members(item.id).catch(() => [])
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name, ownerId: form.ownerId, description: form.description, memberIds: form.memberIds,
    }
    if (editing.value) await portfolioApi.update(form.id, payload)
    else {
      const id = await portfolioApi.create(payload)
      currentId.value = String(id)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    await loadList()
    if (currentId.value) await select(currentId.value)
  } finally {
    saving.value = false
  }
}
async function removePortfolio(item) {
  await ElMessageBox.confirm(`确认删除项目集「${item.name}」？`, '提示', { type: 'warning' })
  await portfolioApi.remove(item.id)
  ElMessage.success('已删除')
  overview.value = null
  currentId.value = null
  loadList()
}

async function openAddProjects() {
  selectedProjectIds.value = []
  // 仅可加入创建人负责∪参与的项目（与已确认口径一致）
  allProjects.value = await portfolioApi.candidateProjects(currentId.value).catch(() => [])
  addDialog.value = true
}
async function confirmAddProjects() {
  if (!selectedProjectIds.value.length) return ElMessage.warning('请选择项目')
  saving.value = true
  try {
    await portfolioApi.addProjects(currentId.value, selectedProjectIds.value)
    ElMessage.success('已添加')
    addDialog.value = false
    await select(currentId.value)
    await loadList()
  } finally {
    saving.value = false
  }
}
async function removeProject(row) {
  await ElMessageBox.confirm(`确认将「${row.name}」移出本项目集？`, '提示', { type: 'warning' })
  await portfolioApi.removeProject(currentId.value, row.id)
  ElMessage.success('已移出')
  await select(currentId.value)
  await loadList()
}

onMounted(async () => {
  members.value = await fetchMembers().catch(() => [])
  await loadList()
  if (portfolios.value.length) select(portfolios.value[0].id)
})
</script>

<style scoped>
.pf-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.pf {
  display: flex;
  gap: var(--mido-space-4);
  flex: 1;
  min-height: 0;
}
.pf__side {
  width: 260px;
  flex-shrink: 0;
}
.pf__main {
  flex: 1;
  min-width: 0;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
.bar__right {
  display: flex;
  gap: var(--mido-space-2);
}
.pf__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pf__count {
  color: var(--el-text-color-secondary);
  font-size: var(--mido-font-size-secondary);
}
.pf__stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
}
.full {
  width: 100%;
}
</style>
