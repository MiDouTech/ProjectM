<template>
  <div class="wecom-cb">
    <el-result v-if="error" icon="error" title="企微登录失败" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/login')">返回登录</el-button>
      </template>
    </el-result>
    <div v-else v-loading="true" class="wecom-cb__loading">企微登录中…</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/org'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const error = ref('')

onMounted(async () => {
  const code = route.query.code
  if (!code) {
    error.value = '缺少授权 code'
    return
  }
  try {
    const data = await authApi.wecomLogin(code)
    useUserStore().setToken(data.token)
    router.push('/')
  } catch (e) {
    error.value = e?.message || '授权失败，请重试'
  }
})
</script>

<style scoped>
.wecom-cb {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
}
.wecom-cb__loading {
  padding: 40px;
  color: var(--el-text-color-secondary);
}
</style>
