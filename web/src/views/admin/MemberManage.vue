<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">成员管理</h2>
      <div class="bar__right">
        <el-input v-model="keyword" placeholder="搜索用户名" clearable class="bar__search" @keyup.enter="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新建成员</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :size="32" :src="avatarUrls[row.avatar]">{{ (row.name || '?').charAt(0) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column label="部门">
        <template #default="{ row }">{{ deptName(row.deptId) }}</template>
      </el-table-column>
      <el-table-column prop="jobLevel" label="职级" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openAssign(row)">分配角色</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无成员，点击新建" /></template>
    </el-table>

    <!-- 新建/编辑（右抽屉） -->
    <el-drawer v-model="drawer" :title="editing ? '编辑成员' : '新建成员'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" :disabled="editing" placeholder="作为登录账号，11 位" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="editing" placeholder="选填，缺省取手机号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="头像">
          <AvatarUpload v-model="form.avatar" :user-id="form.id || 0" :name="form.name" />
        </el-form-item>
        <el-form-item v-if="!editing" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="form.deptId" :data="deptTree" :props="treeProps"
            node-key="id" check-strictly clearable placeholder="选择部门" />
        </el-form-item>
        <el-form-item label="职级">
          <el-select v-model="form.jobLevel" clearable placeholder="选择职级">
            <el-option v-for="l in jobLevels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="选择状态">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 分配角色 -->
    <el-dialog v-model="assignDialog" title="分配角色" width="var(--mido-login-card-width)">
      <el-select v-model="assignRoleIds" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { userApi, roleApi, deptApi } from '@/api/org'
import { attachmentApi } from '@/api/attachment'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const roles = ref([])
const deptTree = ref([])
const avatarUrls = ref({}) // 头像附件ID → 限时预签名URL（列表展示用，按 ID 去重解析）
const keyword = ref('')
const jobLevels = ['L1', 'L2', 'L3', 'L4']
const treeProps = { label: 'name', children: 'children' }

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, phone: '', username: '', name: '', avatar: null, password: '', deptId: null, jobLevel: '', status: 'active' })
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const assignDialog = ref(false)
const assignRoleIds = ref([])
const assignUserId = ref(null)

const deptMap = computed(() => {
  const map = {}
  const walk = (nodes) => nodes?.forEach((n) => { map[n.id] = n.name; walk(n.children) })
  walk(deptTree.value)
  return map
})
const deptName = (id) => deptMap.value[id] || '—'

async function load() {
  loading.value = true
  try {
    const res = await userApi.query({ page: 1, size: 100, username: keyword.value || undefined })
    rows.value = res.list || []
    resolveAvatars()
  } finally {
    loading.value = false
  }
}
// 解析当前页头像 URL（按附件 ID 去重，仅解析有头像的成员）
async function resolveAvatars() {
  const ids = [...new Set(rows.value.map((r) => r.avatar).filter(Boolean))].filter((id) => !avatarUrls.value[id])
  await Promise.all(ids.map(async (id) => {
    try { avatarUrls.value[id] = await attachmentApi.downloadUrl(id) } catch { /* 忽略单个失败 */ }
  }))
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, phone: '', username: '', name: '', avatar: null, password: '', deptId: null, jobLevel: '', status: 'active' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, { id: row.id, phone: row.phone, username: row.username, name: row.name, avatar: row.avatar, password: '', deptId: row.deptId, jobLevel: row.jobLevel, status: row.status })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await userApi.update(form.id, { name: form.name, avatar: form.avatar, deptId: form.deptId, jobLevel: form.jobLevel, status: form.status })
    } else {
      await userApi.create({ phone: form.phone, username: form.username || undefined, name: form.name, avatar: form.avatar, password: form.password, deptId: form.deptId, jobLevel: form.jobLevel, status: form.status })
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除成员「${row.name}」？`, '提示', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

function openAssign(row) {
  assignUserId.value = row.id
  assignRoleIds.value = []
  assignDialog.value = true
}
async function saveAssign() {
  saving.value = true
  try {
    await userApi.assignRoles(assignUserId.value, assignRoleIds.value)
    ElMessage.success('已分配角色')
    assignDialog.value = false
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  deptTree.value = await deptApi.tree()
  roles.value = await roleApi.list()
  load()
})
</script>

<style scoped>
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
.bar__search {
  width: var(--mido-login-card-width);
  max-width: 100%;
}
</style>
