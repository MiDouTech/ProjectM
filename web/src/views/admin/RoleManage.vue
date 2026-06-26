<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">角色管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建角色</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="角色名" />
      <el-table-column prop="code" label="角色编码" />
      <el-table-column label="操作" width="400">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openPerms(row)">功能权限</el-button>
          <el-button link type="primary" @click="openScopes(row)">数据范围</el-button>
          <el-button link type="primary" @click="openFieldPerms(row)">字段权限</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无角色，点击新建" /></template>
    </el-table>

    <!-- 新建/编辑 -->
    <el-drawer v-model="drawer" :title="editing ? '编辑角色' : '新建角色'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 功能权限 -->
    <el-drawer v-model="permsDrawer" title="功能权限（权限码）" size="var(--mido-drawer-width)">
      <p class="mido-text-secondary">输入权限码后回车添加，如 org:user:query</p>
      <el-select v-model="permCodes" multiple filterable allow-create default-first-option
        :reserve-keyword="false" placeholder="输入权限码回车添加" class="full" />
      <template #footer>
        <el-button @click="permsDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePerms">保存</el-button>
      </template>
    </el-drawer>

    <!-- 数据范围 -->
    <el-drawer v-model="scopesDrawer" title="数据范围" size="var(--mido-drawer-width)">
      <div v-for="(item, idx) in scopes" :key="idx" class="ds-row">
        <el-select v-model="item.resource" filterable allow-create default-first-option
          placeholder="资源" class="ds-row__resource">
          <el-option v-for="r in DATA_SCOPE_RESOURCES" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-select v-model="item.scope" placeholder="数据范围" class="ds-row__scope">
          <el-option v-for="s in DATA_SCOPES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button link type="danger" @click="scopes.splice(idx, 1)">删除</el-button>
      </div>
      <el-button :icon="Plus" @click="scopes.push({ resource: '', scope: 'self' })">添加范围</el-button>
      <template #footer>
        <el-button @click="scopesDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScopes">保存</el-button>
      </template>
    </el-drawer>

    <!-- 字段权限：按资源/字段配置「仅查看 / 可编辑」（默认未配置=可编辑） -->
    <el-drawer v-model="fieldPermsDrawer" title="字段权限（仅查看 / 可编辑）" size="var(--mido-drawer-width)">
      <p class="mido-text-secondary">仅需收紧的字段才配置；未列出的字段默认可编辑。多角色合并时取最宽。</p>
      <div v-for="(item, idx) in fieldPerms" :key="idx" class="ds-row">
        <el-select v-model="item.resource" placeholder="资源" class="ds-row__scope" @change="item.field = ''">
          <el-option v-for="r in FIELD_PERM_RESOURCES" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-select v-model="item.field" filterable placeholder="字段" class="ds-row__resource">
          <el-option v-for="f in fieldsOf(item.resource)" :key="f.value" :label="f.label" :value="f.value" />
        </el-select>
        <el-select v-model="item.access" placeholder="权限" class="ds-row__scope">
          <el-option v-for="a in FIELD_ACCESS" :key="a.value" :label="a.label" :value="a.value" />
        </el-select>
        <el-button link type="danger" @click="fieldPerms.splice(idx, 1)">删除</el-button>
      </div>
      <el-button :icon="Plus" @click="fieldPerms.push({ resource: 'task', field: '', access: 'view' })">添加字段</el-button>
      <template #footer>
        <el-button @click="fieldPermsDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFieldPerms">保存</el-button>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { roleApi, DATA_SCOPES, DATA_SCOPE_RESOURCES, FIELD_ACCESS, FIELD_PERM_RESOURCES } from '@/api/org'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])

const drawer = ref(false)
const editing = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', code: '' })
const rules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const permsDrawer = ref(false)
const permCodes = ref([])
const scopesDrawer = ref(false)
const scopes = ref([])
const fieldPermsDrawer = ref(false)
const fieldPerms = ref([])
const currentRoleId = ref(null)

function fieldsOf(resource) {
  return FIELD_PERM_RESOURCES.find((r) => r.value === resource)?.fields || []
}

async function load() {
  loading.value = true
  try {
    rows.value = await roleApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, name: '', code: '' })
  drawer.value = true
}
function openEdit(row) {
  editing.value = true
  Object.assign(form, { id: row.id, name: row.name, code: row.code })
  drawer.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) await roleApi.update(form.id, { name: form.name, code: form.code })
    else await roleApi.create({ name: form.name, code: form.code })
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function openPerms(row) {
  currentRoleId.value = row.id
  permCodes.value = await roleApi.getPerms(row.id)
  permsDrawer.value = true
}
async function savePerms() {
  saving.value = true
  try {
    await roleApi.savePerms(currentRoleId.value, permCodes.value)
    ElMessage.success('已保存权限')
    permsDrawer.value = false
  } finally {
    saving.value = false
  }
}

async function openScopes(row) {
  currentRoleId.value = row.id
  scopes.value = await roleApi.getDataScopes(row.id)
  scopesDrawer.value = true
}
async function saveScopes() {
  saving.value = true
  try {
    await roleApi.saveDataScopes(currentRoleId.value, scopes.value)
    ElMessage.success('已保存数据范围')
    scopesDrawer.value = false
  } finally {
    saving.value = false
  }
}

async function openFieldPerms(row) {
  currentRoleId.value = row.id
  fieldPerms.value = await roleApi.getFieldPerms(row.id)
  fieldPermsDrawer.value = true
}
async function saveFieldPerms() {
  saving.value = true
  try {
    const valid = fieldPerms.value.filter((f) => f.resource && f.field && f.access)
    await roleApi.saveFieldPerms(currentRoleId.value, valid)
    ElMessage.success('已保存字段权限')
    fieldPermsDrawer.value = false
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
.full {
  width: 100%;
}
.ds-row {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-2);
}
.ds-row__resource {
  flex: 1;
}
.ds-row__scope {
  width: var(--mido-admin-nav-width);
}
</style>
