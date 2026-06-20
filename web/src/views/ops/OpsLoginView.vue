<template>
  <div class="login">
    <div class="login__bg" aria-hidden="true"></div>
    <div class="login__scrim" aria-hidden="true"></div>
    <div class="login__glow" aria-hidden="true"></div>

    <main class="login__stage">
      <section class="login__card">
        <header class="login__head">
          <div class="login__logo-fallback">
            <el-icon><Platform /></el-icon>
            <span>米多运营</span>
          </div>
          <h1 class="login__title">平台运营后台</h1>
          <p class="login__sub">米多 · 平台运营管理控制台</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="运营登录名"
              aria-label="运营登录名"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="密码"
              aria-label="密码"
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login__btn"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <footer class="login__feats">
          <span class="login__feat">默认账号 superadmin / superadmin123</span>
        </footer>
      </section>

      <p class="login__copyright">© 2026 米多 · 平台运营后台</p>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Platform, User, Lock } from '@element-plus/icons-vue'
import { opsAuthApi } from '@/api/ops'
import { useOpsUserStore } from '@/store/opsUser'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'superadmin', password: '' })
const rules = {
  username: [{ required: true, message: '请输入运营登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await opsAuthApi.login({ username: form.username, password: form.password })
    useOpsUserStore().setToken(data.token)
    router.push('/ops')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  position: relative;
  min-height: 100dvh;
  height: 100%;
  overflow: hidden;
  background-color: var(--mido-nav-bg);
}

.login__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: url('/login-bg.jpg');
  background-size: cover;
  background-position: center;
}

.login__scrim {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      125% 125% at 50% 42%,
      transparent 0%,
      color-mix(in srgb, var(--mido-nav-bg) 52%, transparent) 66%,
      color-mix(in srgb, var(--mido-nav-bg) 86%, transparent) 100%
    ),
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--mido-nav-bg) 64%, transparent) 0%,
      color-mix(in srgb, var(--el-color-primary-dark-2) 40%, transparent) 54%,
      color-mix(in srgb, var(--mido-cat-s) 58%, transparent) 100%
    );
}

.login__glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(
    42% 40% at 50% 43%,
    color-mix(in srgb, var(--mido-nav-text-active) 12%, transparent) 0%,
    transparent 70%
  );
}

.login__stage {
  position: relative;
  z-index: 1;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--mido-space-6);
}

.login__card {
  width: var(--mido-login-card-width);
  max-width: calc(100vw - var(--mido-space-6) * 2);
  padding: var(--mido-space-6) var(--mido-space-5) var(--mido-space-5);
  background-color: color-mix(in srgb, var(--el-bg-color) 90%, transparent);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  backdrop-filter: blur(20px) saturate(1.4);
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--mido-nav-text-active) 55%, transparent);
  box-shadow: 0 24px 60px -12px color-mix(in srgb, var(--mido-nav-bg) 60%, transparent);
  animation: card-rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login__head {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: var(--mido-space-6);
}

.login__logo-fallback {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
  margin-bottom: var(--mido-space-4);
  font-size: var(--mido-font-size-h2);
  font-weight: var(--mido-font-weight-bold);
  letter-spacing: 2px;
  color: var(--el-color-primary);
}

.login__title {
  margin: 0;
  font-size: var(--mido-font-size-h1);
  line-height: var(--mido-line-height-h1);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}

.login__sub {
  margin: var(--mido-space-1) 0 0;
  font-size: var(--mido-font-size-secondary);
  line-height: var(--mido-line-height-secondary);
  color: var(--el-text-color-secondary);
}

.login__btn {
  width: 100%;
  margin-top: var(--mido-space-2);
  font-weight: var(--mido-font-weight-bold);
  letter-spacing: 4px;
  background: linear-gradient(
    135deg,
    var(--el-color-primary) 0%,
    var(--el-color-primary-dark-2) 100%
  );
  border: none;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.login__btn:hover {
  box-shadow: var(--mido-shadow-pop);
  transform: translateY(-1px);
}

.login__feats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--mido-space-2);
  margin-top: var(--mido-space-5);
}

.login__feat {
  font-size: var(--mido-font-size-caption);
  line-height: var(--mido-line-height-caption);
  color: var(--el-text-color-secondary);
}

.login__copyright {
  margin-top: var(--mido-space-5);
  font-size: var(--mido-font-size-caption);
  line-height: var(--mido-line-height-caption);
  text-align: center;
  color: color-mix(in srgb, var(--mido-nav-text-active) 78%, transparent);
}

@keyframes card-rise {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login__card {
    animation: none;
  }
}

@media (max-width: 480px) {
  .login__stage {
    padding: var(--mido-space-4);
  }

  .login__card {
    padding: var(--mido-space-5) var(--mido-space-4) var(--mido-space-4);
  }
}
</style>
