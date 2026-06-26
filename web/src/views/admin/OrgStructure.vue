<template>
  <el-card shadow="never">
    <div class="bar">
      <div>
        <h2 class="mido-h2">企业微信集成</h2>
        <p class="mido-text-secondary hint">
          配置企业微信凭证与开关；通讯录同步以企微为源，启用后可一键同步部门与成员到本地。
        </p>
      </div>
      <el-tooltip
        :content="status.contactsEnabled ? '从企微通讯录同步部门与成员' : '请先在下方配置并启用「通讯录同步」'"
        placement="bottom"
      >
        <span>
          <el-button
            type="primary"
            :icon="Refresh"
            :loading="syncing"
            :disabled="!status.contactsEnabled"
            @click="onSync"
          >企微同步</el-button>
        </span>
      </el-tooltip>
    </div>

    <el-alert
      v-if="form.lastSyncResult"
      class="sync-result"
      type="success"
      :closable="false"
      :title="`最近同步：${form.lastSyncAt || ''} ｜ ${form.lastSyncResult}`"
    />

    <el-form label-width="120px" class="form" v-loading="loading">
      <el-divider content-position="left">基础</el-divider>
      <el-form-item label="企业 ID（CorpID）">
        <el-input v-model="form.corpId" placeholder="企业微信「我的企业」中的企业 ID（ww 开头）" clearable />
      </el-form-item>

      <el-divider content-position="left">通讯录同步</el-divider>
      <el-form-item label="启用">
        <el-switch v-model="form.contactsEnabled" />
      </el-form-item>
      <el-form-item label="通讯录 Secret">
        <el-input
          v-model="form.contactsSecret"
          type="password"
          show-password
          :placeholder="secretPlaceholder(config.contactsSecretSet)"
        />
      </el-form-item>

      <el-divider content-position="left">扫码登录（SSO）</el-divider>
      <el-form-item label="启用">
        <el-switch v-model="form.ssoEnabled" />
      </el-form-item>
      <el-form-item label="AgentId">
        <el-input v-model="form.ssoAgentId" placeholder="自建应用 AgentId" clearable />
      </el-form-item>
      <el-form-item label="SSO Secret">
        <el-input
          v-model="form.ssoSecret"
          type="password"
          show-password
          :placeholder="secretPlaceholder(config.ssoSecretSet)"
        />
      </el-form-item>

      <el-divider content-position="left">消息推送</el-divider>
      <el-form-item label="启用">
        <el-switch v-model="form.msgEnabled" />
      </el-form-item>
      <el-form-item label="AgentId">
        <el-input v-model="form.msgAgentId" placeholder="自建应用 AgentId" clearable />
      </el-form-item>
      <el-form-item label="消息 Secret">
        <el-input
          v-model="form.msgSecret"
          type="password"
          show-password
          :placeholder="secretPlaceholder(config.msgSecretSet)"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
        <span class="mido-text-secondary tip">Secret 加密存储、不回显；留空表示保持原值不变。</span>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { wecomConfigApi, userApi } from '@/api/org'

const loading = ref(false)
const saving = ref(false)
const syncing = ref(false)
const config = reactive({ contactsSecretSet: false, ssoSecretSet: false, msgSecretSet: false })
const status = reactive({ contactsEnabled: false, ssoEnabled: false, msgEnabled: false })
const form = reactive({
  corpId: '',
  contactsEnabled: false,
  contactsSecret: '',
  ssoEnabled: false,
  ssoAgentId: '',
  ssoSecret: '',
  msgEnabled: false,
  msgAgentId: '',
  msgSecret: '',
  lastSyncAt: '',
  lastSyncResult: '',
})

const secretPlaceholder = (set) => (set ? '已配置，留空不修改' : '未配置')

async function load() {
  loading.value = true
  try {
    const [cfg, st] = await Promise.all([wecomConfigApi.get(), wecomConfigApi.status()])
    Object.assign(config, cfg)
    Object.assign(status, st)
    Object.assign(form, {
      corpId: cfg.corpId || '',
      contactsEnabled: cfg.contactsEnabled,
      ssoEnabled: cfg.ssoEnabled,
      ssoAgentId: cfg.ssoAgentId || '',
      msgEnabled: cfg.msgEnabled,
      msgAgentId: cfg.msgAgentId || '',
      lastSyncAt: cfg.lastSyncAt || '',
      lastSyncResult: cfg.lastSyncResult || '',
      // secret 不回显，清空输入
      contactsSecret: '',
      ssoSecret: '',
      msgSecret: '',
    })
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await wecomConfigApi.save({
      corpId: form.corpId,
      contactsEnabled: form.contactsEnabled,
      contactsSecret: form.contactsSecret,
      ssoEnabled: form.ssoEnabled,
      ssoAgentId: form.ssoAgentId,
      ssoSecret: form.ssoSecret,
      msgEnabled: form.msgEnabled,
      msgAgentId: form.msgAgentId,
      msgSecret: form.msgSecret,
    })
    ElMessage.success('已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function onSync() {
  syncing.value = true
  try {
    const res = await userApi.syncWecomContacts()
    ElMessage.success(`同步完成：部门 ${res.deptCount ?? '-'} / 成员新增 ${res.userCreated ?? '-'}、更新 ${res.userUpdated ?? '-'}`)
    await load()
  } finally {
    syncing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--mido-space-4);
  margin-bottom: var(--mido-space-4);
}

.hint {
  margin: var(--mido-space-1) 0 0;
}

.sync-result {
  margin-bottom: var(--mido-space-4);
}

.form {
  max-width: 640px;
}

.tip {
  margin-left: var(--mido-space-3);
}
</style>
