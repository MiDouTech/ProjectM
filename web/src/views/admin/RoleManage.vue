<template>
  <el-card shadow="never">
    <div class="bar">
      <h2 class="mido-h2">角色管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建角色</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <div class="role-name">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.code === 'admin'" size="small" type="info" effect="plain">内置</el-tag>
            <span class="mido-mono mido-text-secondary role-name__code">{{ row.code }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="400">
        <template #default="{ row }">
          <RowActions>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openPerms(row)">功能权限</el-button>
            <el-button link type="primary" @click="openScopes(row)">数据范围</el-button>
            <el-button link type="primary" @click="openFieldPerms(row)">字段权限</el-button>
            <el-button link type="danger" :disabled="row.code === 'admin'" @click="remove(row)">删除</el-button>
          </RowActions>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无角色，点击新建" /></template>
    </el-table>

    <!-- 新建/编辑 -->
    <el-drawer v-model="drawer" :title="editing ? '编辑角色' : '新建角色'" size="var(--mido-drawer-width)">
      <el-form ref="formRef" :model="form" :rules="rules" :label-width="80">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" placeholder="如：项目经理" /></el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="form.code" :disabled="editing" placeholder="如：pm，仅英文/数字" />
          <div class="mido-text-secondary form-hint">系统内部使用，创建后不可更改。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 功能权限：勾选该角色可使用的功能（中文分组目录，不暴露权限码） -->
    <el-drawer v-model="permsDrawer" title="功能权限" size="var(--mido-drawer-width)">
      <p class="mido-text-secondary">勾选该角色可使用的功能。</p>
      <div v-for="g in PERM_CATALOG" :key="g.group" class="perm-group">
        <div class="perm-group__title mido-h2">{{ g.group }}</div>
        <el-checkbox-group v-model="permCodes" class="perm-group__items">
          <el-checkbox v-for="p in g.perms" :key="p.code" :value="p.code">{{ p.name }}</el-checkbox>
        </el-checkbox-group>
      </div>
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

      <div class="custom-dept">
        <p class="mido-text-secondary">自定义部门集（任一资源数据范围选「自定义」时，按下列部门可见）</p>
        <el-select v-model="customDepts" multiple filterable placeholder="选择部门" class="full">
          <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </div>

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
import RowActions from '@/components/RowActions.vue'
import { roleApi, deptApi, DATA_SCOPES, DATA_SCOPE_RESOURCES, FIELD_ACCESS, FIELD_PERM_RESOURCES } from '@/api/org'

// 功能权限目录：中文分组呈现，屏蔽权限码技术细节。code 与后端 @PreAuthorize 对齐。
const PERM_CATALOG = [
  {
    group: '成员与组织', perms: [
      { code: 'org:user:query', name: '查看成员' },
      { code: 'org:user:create', name: '管理成员（新增 / 编辑 / 停用）' },
      { code: 'org:dept:create', name: '管理部门' },
      { code: 'org:role:create', name: '管理角色与权限' },
    ],
  },
  {
    group: '系统与运维', perms: [
      { code: 'org:config:manage', name: '管理系统配置（项目类型 / 状态 / 字段等）' },
      { code: 'org:audit:query', name: '查看操作日志' },
      { code: 'org:apikey:manage', name: '管理开放平台（API 密钥）' },
    ],
  },
]
const CATALOG_CODES = new Set(PERM_CATALOG.flatMap((g) => g.perms.map((p) => p.code)))

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
// 目录外的历史权限码：勾选界面不展示，但保存时原样保留，避免误删
const extraCodes = ref([])
const scopesDrawer = ref(false)
const scopes = ref([])
const customDepts = ref([])
const deptOptions = ref([])
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
  const all = (await roleApi.getPerms(row.id)) || []
  // 目录内的进勾选区，目录外的暂存以便保存时保留
  permCodes.value = all.filter((c) => CATALOG_CODES.has(c))
  extraCodes.value = all.filter((c) => !CATALOG_CODES.has(c))
  permsDrawer.value = true
}
async function savePerms() {
  saving.value = true
  try {
    await roleApi.savePerms(currentRoleId.value, [...permCodes.value, ...extraCodes.value])
    ElMessage.success('已保存权限')
    permsDrawer.value = false
  } finally {
    saving.value = false
  }
}

function flattenDepts(nodes, acc = []) {
  for (const n of nodes || []) {
    acc.push({ id: n.id, name: n.name })
    if (n.children?.length) flattenDepts(n.children, acc)
  }
  return acc
}

async function openScopes(row) {
  currentRoleId.value = row.id
  const [scopeList, customList, tree] = await Promise.all([
    roleApi.getDataScopes(row.id),
    roleApi.getCustomDepts(row.id),
    deptApi.tree(),
  ])
  scopes.value = scopeList
  customDepts.value = customList
  deptOptions.value = flattenDepts(tree)
  scopesDrawer.value = true
}
async function saveScopes() {
  saving.value = true
  try {
    await roleApi.saveDataScopes(currentRoleId.value, scopes.value)
    await roleApi.saveCustomDepts(currentRoleId.value, customDepts.value)
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
.role-name {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.role-name__code {
  font-size: var(--mido-font-size-caption);
}
.form-hint {
  margin-top: var(--mido-space-1);
  font-size: var(--mido-font-size-caption);
}
.perm-group {
  margin-bottom: var(--mido-space-4);
}
.perm-group__title {
  margin-bottom: var(--mido-space-2);
}
.perm-group__items {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-2);
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
.custom-dept {
  margin-top: var(--mido-space-4);
}
</style>
