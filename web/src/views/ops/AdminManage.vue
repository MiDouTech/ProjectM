<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">运营账号</h2>
      <el-button type="primary" :icon="Plus" :disabled="!ops.hasPerm('platform:admin:manage')" @click="openCreate">新建账号</el-button>
    </div>

    <ErrorState v-if="loadError" @retry="load" />
    <el-table v-else v-loading="loading" :data="rows" stripe>
      <el-table-column prop="username" label="登录名" width="160" />
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">{{ (row.roleNames || []).join('，') || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="最后登录" width="180">
        <template #default="{ row }">{{ row.lastLoginAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!ops.hasPerm('platform:admin:manage')" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" :disabled="!ops.hasPerm('platform:admin:manage')" @click="openResetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无运营账号，点击新建" /></template>
    </el-table>

    <!-- 新建 / 编辑（右抽屉）-->
    <el-drawer v-model="drawer" :title="editing ? '编辑账号' : '新建账号'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" :disabled="editing" />
        </el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!editing" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item v-if="editing" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ENABLE_STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialog" title="重置密码" width="var(--mido-login-card-width)">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" :label-width="80">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveResetPwd">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import { platformAdminApi, ENABLE_STATUS } from '@/api/ops'
import { useOpsUserStore } from '@/store/opsUser'

const ops = useOpsUserStore()

// 密码强度：8-64 位且同时含字母和数字（与后端 PasswordPolicy 一致）
const PWD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/

const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const rows = ref([])
const roles = ref([])

async function load() {
  loading.value = true
  loadError.value = false
  try {
    if (roles.value.length === 0) {
      roles.value = await platformAdminApi.roles()
    }
    rows.value = await platformAdminApi.list()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, username: '', name: '', password: '', status: 'active', roleIds: [] })
const rules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: PWD_REGEX, message: '密码需 8-64 位且同时包含字母和数字', trigger: 'blur' },
  ],
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, username: '', name: '', password: '', status: 'active', roleIds: [] })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, {
    id: row.id, username: row.username, name: row.name, password: '',
    status: row.status, roleIds: [...(row.roleIds || [])],
  })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await platformAdminApi.update(form.id, { name: form.name, status: form.status, roleIds: form.roleIds })
    } else {
      await platformAdminApi.create({ username: form.username, name: form.name, password: form.password, roleIds: form.roleIds })
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

/* ===== 重置密码 ===== */
const pwdDialog = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ id: null, password: '' })
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: PWD_REGEX, message: '密码需 8-64 位且同时包含字母和数字', trigger: 'blur' },
  ],
}
function openResetPwd(row) {
  pwdForm.id = row.id
  pwdForm.password = ''
  pwdDialog.value = true
}
async function saveResetPwd() {
  await pwdFormRef.value.validate()
  saving.value = true
  try {
    await platformAdminApi.resetPassword(pwdForm.id, pwdForm.password)
    ElMessage.success('密码已重置')
    pwdDialog.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-4);
}
</style>
