<template>
  <div class="login">
    <el-card class="login__card" shadow="never">
      <div class="login__brand">
        <el-icon class="login__logo"><Grid /></el-icon>
        <h1 class="mido-h1">米多项目管理</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="手机号 / 用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" class="login__btn" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Grid, User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api/org'
import { useUserStore } from '@/store/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '13800000000', password: '' })
const rules = {
  username: [{ required: true, message: '请输入手机号或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await authApi.login({ username: form.username, password: form.password })
    useUserStore().setToken(data.token)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-bg-color-page);
}

.login__card {
  width: var(--mido-login-card-width);
  padding: var(--mido-space-4);
}

.login__brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--mido-space-2);
  color: var(--el-color-primary);
  margin-bottom: var(--mido-space-5);
}

.login__logo {
  font-size: var(--mido-font-size-h1);
}

.login__btn {
  width: 100%;
}
</style>
