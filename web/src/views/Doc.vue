<template>
  <div class="mido-page doc">
    <h1 class="mido-h1">文档中心</h1>
    <p class="mido-text-secondary doc__sub">按项目维护知识库：在线文档、目录、版本历史。</p>

    <div class="doc__body" v-loading="projLoading">
      <!-- 左：我参与的项目 -->
      <el-card shadow="never" class="doc__side">
        <h3 class="mido-h2">我的项目（{{ projects.length }}）</h3>
        <el-menu v-if="projects.length" :default-active="String(activeId)" @select="selectProject">
          <el-menu-item v-for="p in projects" :key="p.id" :index="String(p.id)">
            <span class="doc__proj">
              <CategoryBadge :category="p.category" />
              <span class="doc__proj-name">{{ p.name }}</span>
            </span>
          </el-menu-item>
        </el-menu>
        <el-empty v-else description="你尚未参与任何项目" :image-size="60" />
      </el-card>

      <!-- 中：知识库目录树 -->
      <el-card v-if="activeId" shadow="never" class="doc__tree" v-loading="treeLoading">
        <div class="doc__tree-head">
          <h3 class="mido-h2">知识库</h3>
          <span class="doc__tree-actions">
            <el-button link type="primary" :icon="Folder" @click="addNode('folder', null)">目录</el-button>
            <el-dropdown trigger="click" @command="addDoc">
              <el-button link type="primary" :icon="Document">文档<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="blank">空白文档</el-dropdown-item>
                  <el-dropdown-item v-for="t in templates" :key="t.id" :command="t.id" divided>{{ t.name }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-upload :show-file-list="false" :http-request="uploadFile" :disabled="uploading">
              <el-button link type="primary" :icon="Upload" :loading="uploading">文件</el-button>
            </el-upload>
            <el-button link :icon="Delete" title="回收站" @click="openRecycle" />
          </span>
        </div>
        <el-input v-model="kw" class="doc__search" :prefix-icon="Search" clearable
          placeholder="搜索标题或正文…" @input="onSearch" />

        <!-- 搜索结果 -->
        <div v-if="kw.trim()" class="doc__results">
          <div v-for="r in results" :key="r.id" class="doc__result" @click="openNode(r)">
            <el-icon class="doc__node-icon">
              <Folder v-if="r.type === 'folder'" /><Paperclip v-else-if="r.type === 'file'" /><Document v-else />
            </el-icon>
            <span class="doc__result-main">
              <span class="doc__node-label">{{ r.title }}</span>
              <span v-if="r.snippet" class="mido-text-secondary doc__snippet">{{ r.snippet }}</span>
            </span>
          </div>
          <el-empty v-if="!results.length" description="无匹配结果" :image-size="60" />
        </div>

        <!-- 目录树 -->
        <el-tree v-else-if="tree.length" :data="tree" node-key="id" :props="treeProps" draggable
          :expand-on-click-node="false" :allow-drop="allowDrop" highlight-current
          :current-node-key="current?.id" @node-click="openNode" @node-drop="onDrop">
          <template #default="{ data }">
            <span class="doc__node">
              <el-icon class="doc__node-icon">
                <Folder v-if="data.type === 'folder'" /><Paperclip v-else-if="data.type === 'file'" /><Document v-else />
              </el-icon>
              <span class="doc__node-label">{{ data.title }}</span>
              <span class="doc__node-ops">
                <el-button v-if="data.type === 'folder'" link :icon="Plus" title="在此新建"
                  @click.stop="addNode('doc', data)" />
                <el-button link :icon="EditPen" title="重命名" @click.stop="renameNode(data)" />
                <el-button link :icon="Delete" title="删除" @click.stop="deleteNode(data)" />
              </span>
            </span>
          </template>
        </el-tree>
        <el-empty v-else description="暂无文档，点上方新建" :image-size="60" />
      </el-card>

      <!-- 右：文档编辑 -->
      <el-card v-if="activeId" shadow="never" class="doc__main" v-loading="docLoading">
        <template v-if="current && current.type === 'doc'">
          <div class="doc__editor-head">
            <el-input v-model="title" class="doc__title" placeholder="文档标题" />
            <el-button :icon="current.favorited ? StarFilled : Star"
              :type="current.favorited ? 'warning' : ''" @click="toggleFav">收藏</el-button>
            <el-button :icon="Clock" @click="openHistory">历史</el-button>
            <el-button :icon="Lock" @click="openAcl">权限</el-button>
            <el-button :icon="Share" @click="openShare">分享</el-button>
            <el-dropdown trigger="click" @command="exportDoc">
              <el-button :icon="Download">导出<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="md">Markdown (.md)</el-dropdown-item>
                  <el-dropdown-item command="html">HTML (.html)</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存</el-button>
          </div>
          <DocEditor v-model="content" />
          <div class="doc__comments">
            <h4 class="mido-h2">评论</h4>
            <CommentThread entity-type="doc" :entity-id="current.id" :users="members" />
          </div>
        </template>
        <FilePreview v-else-if="current && current.type === 'file'" :url="fileUrl" :name="current.title" />
        <el-empty v-else-if="current" description="目录节点：在左侧选择或新建文档" :image-size="80" />
        <el-empty v-else description="从左侧选择或新建一篇文档" :image-size="80" />
      </el-card>
    </div>

    <!-- 版本历史抽屉 -->
    <el-drawer v-model="historyOpen" title="版本历史" :size="hasPreview ? 720 : 420">
      <div class="doc__history">
        <el-table :data="versions" size="small" class="doc__history-list">
          <el-table-column label="版本" width="70">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="说明" min-width="120">
            <template #default="{ row }">{{ row.changeNote || '—' }}</template>
          </el-table-column>
          <el-table-column label="时间" width="120">
            <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="preview(row)">预览</el-button>
              <el-button link type="primary" @click="rollback(row)">回滚</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无历史版本" :image-size="60" /></template>
        </el-table>
        <div v-if="hasPreview" class="doc__history-preview">
          <h4 class="mido-h2">预览 v{{ previewVersion?.versionNo }}</h4>
          <DocEditor :model-value="previewContent" :editable="false" />
        </div>
      </div>
    </el-drawer>

    <!-- 回收站抽屉 -->
    <el-drawer v-model="recycleOpen" title="回收站" :size="420">
      <el-table :data="recycleItems" size="small">
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            <el-icon class="doc__node-icon">
              <Folder v-if="row.type === 'folder'" /><Paperclip v-else-if="row.type === 'file'" /><Document v-else />
            </el-icon>
            <span class="doc__recycle-name">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="删除时间" width="120">
          <template #default="{ row }">{{ fmtTime(row.trashedTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="restore(row)">恢复</el-button>
            <el-button link type="danger" @click="purge(row)">彻底删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="回收站为空" :image-size="60" /></template>
      </el-table>
    </el-drawer>

    <!-- 权限 ACL 抽屉 -->
    <el-drawer v-model="aclOpen" title="文档权限" :size="460">
      <div class="doc__acl-add">
        <el-select v-model="aclForm.principalType" class="doc__acl-w">
          <el-option label="成员" value="user" />
          <el-option label="角色" value="role" />
        </el-select>
        <el-select v-model="aclForm.principalId" filterable placeholder="选择" class="doc__acl-w">
          <el-option v-for="o in aclForm.principalType === 'user' ? members : roles"
            :key="o.id" :label="o.name" :value="o.id" />
        </el-select>
        <el-select v-model="aclForm.permission" class="doc__acl-w">
          <el-option label="可读" value="read" />
          <el-option label="可写" value="write" />
          <el-option label="管理" value="admin" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="grantAcl">授权</el-button>
      </div>
      <el-table :data="aclList" size="small">
        <el-table-column label="主体">
          <template #default="{ row }">
            {{ row.principalType === 'user' ? '成员' : '角色' }}：{{ principalName(row) }}
          </template>
        </el-table-column>
        <el-table-column label="权限" width="80">
          <template #default="{ row }">{{ permLabel(row.permission) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="revokeAcl(row)">撤销</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="未设置，默认项目成员可读写" :image-size="60" /></template>
      </el-table>
    </el-drawer>

    <!-- 分享抽屉 -->
    <el-drawer v-model="shareOpen" title="公开分享" :size="460">
      <template v-if="share && share.enabled">
        <el-alert type="success" :closable="false" title="任何人通过链接可只读访问，请谨慎分享" class="doc__share-tip" />
        <el-input :model-value="shareUrl" readonly class="doc__share-url">
          <template #append><el-button :icon="CopyDocument" @click="copyShare">复制</el-button></template>
        </el-input>
        <p v-if="share.expireTime" class="mido-text-secondary">过期时间：{{ fmtTime(share.expireTime) }}</p>
        <el-button type="danger" plain @click="stopShare">停用链接</el-button>
      </template>
      <template v-else>
        <p class="mido-text-secondary">生成公开只读链接，免登录即可查看本文档。</p>
        <el-date-picker v-model="shareExpire" type="datetime" placeholder="过期时间（可选，空=永久）"
          value-format="YYYY-MM-DDTHH:mm:ss" class="doc__share-exp" />
        <el-button type="primary" :icon="Share" @click="makeShare">生成链接</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document, Plus, EditPen, Delete, Check, Clock, Upload, Paperclip,
  Search, Star, StarFilled, Download, ArrowDown, Lock, Share, CopyDocument } from '@element-plus/icons-vue'
import CategoryBadge from '@/components/CategoryBadge.vue'
import DocEditor from '@/components/DocEditor.vue'
import FilePreview from '@/components/FilePreview.vue'
import CommentThread from '@/components/CommentThread.vue'
import { projectApi } from '@/api/project'
import { roleApi } from '@/api/org'
import { docApi } from '@/api/doc'
import { toHtml, toMarkdown, downloadText } from '@/utils/tiptap'

const treeProps = { label: 'title', children: 'children' }

const projLoading = ref(false)
const treeLoading = ref(false)
const docLoading = ref(false)
const saving = ref(false)
const projects = ref([])
const activeId = ref(null)
const tree = ref([])
const current = ref(null) // 当前选中节点详情（DocDetailVO）
const title = ref('')
const content = ref(null) // Tiptap JSON 对象
const fileUrl = ref('') // file 节点的预签名预览 URL
const uploading = ref(false)
const templates = ref([]) // 文档模板库
const members = ref([]) // 项目成员（评论@/授权）
const roles = ref([]) // 角色（授权）
const kw = ref('') // 搜索关键词
const results = ref([]) // 搜索结果
const baseVersionId = ref(null) // 载入时的版本，乐观防冲突

const fmtTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—')

// ===== 项目 / 树 =====
async function loadTree() {
  treeLoading.value = true
  try {
    tree.value = await docApi.tree(activeId.value) || []
  } finally {
    treeLoading.value = false
  }
}
function selectProject(id) {
  activeId.value = Number(id)
  current.value = null
  title.value = ''
  content.value = null
  kw.value = ''
  results.value = []
  loadTree()
  loadMembers()
}
async function loadMembers() {
  try {
    const list = await projectApi.members(activeId.value) || []
    members.value = list.map((m) => ({ id: m.userId ?? m.id, name: m.name ?? m.userName ?? m.realName }))
  } catch {
    members.value = []
  }
}

// 搜索（轻量防抖）
let searchTimer
function onSearch() {
  clearTimeout(searchTimer)
  if (!kw.value.trim()) {
    results.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    results.value = await docApi.search(activeId.value, kw.value.trim()) || []
  }, 250)
}

// ===== 节点操作 =====
async function openNode(data) {
  if (data.type === 'folder') {
    current.value = { id: data.id, type: 'folder', title: data.title }
    return
  }
  if (data.type === 'file') {
    docLoading.value = true
    try {
      fileUrl.value = await docApi.downloadUrl(data.id)
      current.value = { id: data.id, type: 'file', title: data.title }
    } finally {
      docLoading.value = false
    }
    return
  }
  docLoading.value = true
  try {
    const d = await docApi.get(data.id)
    current.value = d
    title.value = d.title
    content.value = d.content ? JSON.parse(d.content) : null
    baseVersionId.value = d.currentVersionId || null
  } finally {
    docLoading.value = false
  }
}

// 上传文件到当前选中目录（否则到根）
async function uploadFile({ file }) {
  uploading.value = true
  try {
    const parentId = current.value && current.value.type === 'folder' ? current.value.id : 0
    await docApi.upload(activeId.value, parentId, file)
    ElMessage.success('已上传')
    await loadTree()
  } finally {
    uploading.value = false
  }
}

async function addNode(type, parent) {
  const label = type === 'folder' ? '目录' : '文档'
  try {
    const { value } = await ElMessageBox.prompt(`请输入${label}名称`, `新建${label}`, {
      confirmButtonText: '创建', cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim() ? true : '名称不能为空'),
    })
    const id = await docApi.create({
      projectId: activeId.value, parentId: parent ? parent.id : 0, type, title: value.trim(),
    })
    ElMessage.success('已创建')
    await loadTree()
    if (type === 'doc') openNode({ id, type: 'doc' })
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

// 从模板/空白新建文档（顶部下拉）
async function addDoc(command) {
  const tpl = command === 'blank' ? null : templates.value.find((t) => t.id === command)
  try {
    const { value } = await ElMessageBox.prompt('请输入文档名称', `新建${tpl ? tpl.name : '文档'}`, {
      confirmButtonText: '创建', cancelButtonText: '取消', inputValue: tpl ? tpl.name : '',
      inputValidator: (v) => (v && v.trim() ? true : '名称不能为空'),
    })
    const parentId = current.value && current.value.type === 'folder' ? current.value.id : 0
    const id = await docApi.create({ projectId: activeId.value, parentId, type: 'doc', title: value.trim() })
    if (tpl && tpl.content) {
      await docApi.saveContent(id, { title: value.trim(), content: tpl.content, contentText: '' })
    }
    ElMessage.success('已创建')
    await loadTree()
    openNode({ id, type: 'doc' })
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function toggleFav() {
  const on = await docApi.toggleFavorite(current.value.id)
  current.value = { ...current.value, favorited: on }
  ElMessage.success(on ? '已收藏' : '已取消收藏')
}

function exportDoc(cmd) {
  const name = (title.value || current.value.title || 'document').trim()
  if (cmd === 'md') {
    downloadText(`${name}.md`, `# ${name}\n\n${toMarkdown(content.value)}`, 'text/markdown')
  } else {
    const html = `<!doctype html><html><head><meta charset="utf-8"><title>${name}</title></head>`
      + `<body>${toHtml(content.value)}</body></html>`
    downloadText(`${name}.html`, html, 'text/html')
  }
}

async function renameNode(data) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名', {
      confirmButtonText: '保存', cancelButtonText: '取消', inputValue: data.title,
      inputValidator: (v) => (v && v.trim() ? true : '名称不能为空'),
    })
    await docApi.rename(data.id, { title: value.trim(), icon: data.icon })
    if (current.value && current.value.id === data.id) title.value = value.trim()
    await loadTree()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function deleteNode(data) {
  try {
    await ElMessageBox.confirm(
      data.type === 'folder' ? '移入回收站将一并回收其下所有内容，确认？' : `将「${data.title}」移入回收站？`,
      '移入回收站', { type: 'warning', confirmButtonText: '移入回收站', cancelButtonText: '取消' })
    await docApi.remove(data.id)
    ElMessage.success('已移入回收站')
    if (current.value && current.value.id === data.id) {
      current.value = null
      content.value = null
    }
    await loadTree()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

// ===== 回收站 =====
const recycleOpen = ref(false)
const recycleItems = ref([])
async function openRecycle() {
  recycleItems.value = await docApi.recycle(activeId.value) || []
  recycleOpen.value = true
}
async function restore(row) {
  await docApi.restore(row.id)
  ElMessage.success('已恢复')
  recycleItems.value = await docApi.recycle(activeId.value) || []
  await loadTree()
}
async function purge(row) {
  try {
    await ElMessageBox.confirm(`彻底删除「${row.title}」后不可恢复，确认？`, '彻底删除',
      { type: 'warning', confirmButtonText: '彻底删除', cancelButtonText: '取消' })
    await docApi.purge(row.id)
    ElMessage.success('已彻底删除')
    recycleItems.value = await docApi.recycle(activeId.value) || []
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

// 拖拽：仅允许放入目录内部；放到文档上只能同级前后
function allowDrop(dragging, drop, type) {
  if (type === 'inner') return drop.data.type === 'folder'
  return true
}
async function onDrop(dragging, drop, dropType) {
  const node = dragging.data
  let parentId = 0
  let siblings = tree.value
  if (dropType === 'inner') {
    parentId = drop.data.id
    siblings = drop.data.children || []
  } else if (drop.parent && drop.parent.level > 0) {
    parentId = drop.parent.data.id
    siblings = drop.parent.data.children || []
  }
  const sortNo = Math.max(0, siblings.findIndex((s) => s.id === node.id))
  try {
    await docApi.move(node.id, { parentId, sortNo })
  } catch (e) {
    await loadTree() // 失败回滚视图
    throw e
  }
}

// ===== 保存正文 =====
function extractText(node) {
  if (!node) return ''
  if (node.text) return node.text
  if (Array.isArray(node.content)) return node.content.map(extractText).join(node.type === 'paragraph' ? '' : ' ')
  return ''
}
async function save() {
  if (!current.value || current.value.type !== 'doc') return
  saving.value = true
  try {
    const ver = await docApi.saveContent(current.value.id, {
      title: title.value.trim() || current.value.title,
      content: content.value ? JSON.stringify(content.value) : null,
      contentText: extractText(content.value).slice(0, 2000),
      baseVersionId: baseVersionId.value,
    })
    baseVersionId.value = ver?.id || baseVersionId.value
    current.value = { ...current.value, currentVersionId: baseVersionId.value }
    ElMessage.success('已保存')
    await loadTree()
  } finally {
    saving.value = false
  }
}

// ===== 版本历史 =====
const historyOpen = ref(false)
const versions = ref([])
const previewVersion = ref(null)
const previewContent = ref(null)
const hasPreview = ref(false)
async function openHistory() {
  if (!current.value) return
  previewVersion.value = null
  hasPreview.value = false
  versions.value = await docApi.versions(current.value.id) || []
  historyOpen.value = true
}
async function preview(row) {
  const v = await docApi.versionContent(row.id)
  previewVersion.value = row
  previewContent.value = v.content ? JSON.parse(v.content) : null
  hasPreview.value = true
}
async function rollback(row) {
  try {
    await ElMessageBox.confirm(`确认回滚到 v${row.versionNo}？将作为新版本追加，不影响历史。`, '回滚确认',
      { type: 'warning', confirmButtonText: '回滚', cancelButtonText: '取消' })
    await docApi.rollback(current.value.id, row.id)
    ElMessage.success('已回滚')
    historyOpen.value = false
    await openNode({ id: current.value.id, type: 'doc' })
    await loadTree()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

// ===== 权限 ACL =====
const aclOpen = ref(false)
const aclList = ref([])
const aclForm = ref({ principalType: 'user', principalId: null, permission: 'read' })
const permLabel = (p) => ({ read: '可读', write: '可写', admin: '管理' }[p] || p)
function principalName(row) {
  const pool = row.principalType === 'user' ? members.value : roles.value
  return pool.find((o) => o.id === row.principalId)?.name || row.principalId
}
async function openAcl() {
  aclForm.value = { principalType: 'user', principalId: null, permission: 'read' }
  aclList.value = await docApi.listAcl(current.value.id) || []
  aclOpen.value = true
}
async function grantAcl() {
  if (!aclForm.value.principalId) {
    ElMessage.warning('请选择授权对象')
    return
  }
  await docApi.grantAcl(current.value.id, { ...aclForm.value })
  ElMessage.success('已授权')
  aclList.value = await docApi.listAcl(current.value.id) || []
}
async function revokeAcl(row) {
  await docApi.revokeAcl(row.id)
  aclList.value = await docApi.listAcl(current.value.id) || []
}

// ===== 分享 =====
const shareOpen = ref(false)
const share = ref(null)
const shareExpire = ref(null)
const shareUrl = computed(() => (share.value ? `${location.origin}/share/${share.value.token}` : ''))
async function openShare() {
  shareExpire.value = null
  share.value = await docApi.getShare(current.value.id)
  shareOpen.value = true
}
async function makeShare() {
  share.value = await docApi.createShare(current.value.id, shareExpire.value)
  ElMessage.success('已生成分享链接')
}
async function stopShare() {
  await docApi.disableShare(current.value.id)
  share.value = null
  ElMessage.success('已停用')
}
async function copyShare() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(async () => {
  projLoading.value = true
  try {
    docApi.templates().then((t) => { templates.value = t || [] }).catch(() => {})
    roleApi.list().then((r) => { roles.value = r || [] }).catch(() => {})
    projects.value = await projectApi.mine() || []
    if (projects.value.length) selectProject(projects.value[0].id)
  } finally {
    projLoading.value = false
  }
})
</script>

<style scoped>
.doc__sub {
  margin: calc(-1 * var(--mido-space-2)) 0 var(--mido-space-4);
}
.doc__body {
  display: flex;
  gap: var(--mido-space-4);
  align-items: stretch;
  min-height: 60vh;
}
.doc__side {
  width: var(--mido-admin-nav-width);
  flex: none;
}
.doc__tree {
  width: var(--mido-drawer-width);
  flex: none;
}
.doc__tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--mido-space-2);
}
.doc__tree-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-1);
}
.doc__recycle-name {
  margin-left: var(--mido-space-1);
}
.doc__search {
  margin-bottom: var(--mido-space-2);
}
.doc__results {
  display: flex;
  flex-direction: column;
}
.doc__result {
  display: flex;
  align-items: flex-start;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2);
  border-radius: var(--mido-radius-sm);
  cursor: pointer;
}
.doc__result:hover {
  background-color: var(--el-fill-color-light);
}
.doc__result-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.doc__snippet {
  font-size: var(--mido-font-size-caption);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc__comments {
  margin-top: var(--mido-space-5);
  border-top: var(--mido-border-width) solid var(--el-border-color-light);
  padding-top: var(--mido-space-3);
}
.doc__acl-add {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-3);
}
.doc__acl-w {
  width: 120px;
}
.doc__share-tip,
.doc__share-url,
.doc__share-exp {
  margin-bottom: var(--mido-space-3);
}
.doc__share-exp {
  width: 100%;
}
.doc__main {
  flex: 1;
  min-width: 0;
}
.doc__proj {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
}
.doc__proj-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc__node {
  display: inline-flex;
  align-items: center;
  gap: var(--mido-space-2);
  width: 100%;
}
.doc__node-icon {
  color: var(--el-text-color-secondary);
}
.doc__node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc__node-ops {
  display: none;
}
.doc__node:hover .doc__node-ops {
  display: inline-flex;
}
.doc__editor-head {
  display: flex;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-3);
}
.doc__title {
  flex: 1;
}
.doc__history {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-4);
}
.doc__history-preview {
  border-top: var(--mido-border-width) solid var(--el-border-color-light);
  padding-top: var(--mido-space-3);
}
</style>
