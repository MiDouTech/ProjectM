<template>
  <div class="login">
    <!-- 品牌墙 -->
    <aside class="login__brand-panel">
      <div class="login__brand-deco" aria-hidden="true"></div>
      <div class="login__brand-content">
        <div class="login__brand-top">
          <el-icon class="login__brand-logo"><Grid /></el-icon>
          <span class="login__brand-name">米多项目管理</span>
        </div>
        <div class="login__brand-hero">
          <h2 class="login__brand-title">让每一个项目<br />有据可依 · 有值可验</h2>
          <p class="login__brand-sub">立项审批 · 干系人管理 · NPSS 价值验收</p>
        </div>
        <p class="login__brand-foot">© 2026 米多 · 通用项目管理系统</p>
      </div>
    </aside>

    <!-- 登录区 -->
    <main class="login__form-panel">
      <el-card class="login__card" shadow="never">
        <div class="login__welcome">
          <div class="login__welcome-mark"><el-icon><Grid /></el-icon></div>
          <h1 class="login__welcome-title">欢迎回来</h1>
          <p class="login__welcome-sub">登录以继续你的工作台</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="手机号 / 用户名"
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
            登录
          </el-button>
        </el-form>
      </el-card>
    </main>
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
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  background-color: var(--el-bg-color);
}

/* ===== 品牌墙 ===== */
.login__brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--mido-space-6);
  /* 深海军蓝 → 主蓝 → 战略紫，斜向渐变，沉稳高级 */
  background: linear-gradient(
    135deg,
    var(--mido-nav-bg) 0%,
    var(--el-color-primary-dark-2) 55%,
    var(--mido-cat-s) 130%
  );
  color: var(--mido-nav-text-active);
}

/* 柔光装饰圆斑：用 color-mix 从 token 派生半透明，不写裸 rgba */
.login__brand-deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      40% 40% at 78% 18%,
      color-mix(in srgb, var(--mido-nav-text-active) 22%, transparent) 0%,
      transparent 70%
    ),
    radial-gradient(
      36% 36% at 12% 88%,
      color-mix(in srgb, var(--mido-cat-s) 55%, transparent) 0%,
      transparent 70%
    );
}

.login__brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-6);
  height: 100%;
  justify-content: space-between;
}

.login__brand-top {
  display: flex;
  align-items: center;
  gap: var(--mido-space-2);
}

.login__brand-logo {
  /* 品牌 Logo 视觉尺寸，复用间距 token 作字号 */
  font-size: var(--mido-space-6);
}

.login__brand-name {
  font-size: var(--mido-font-size-h2);
  font-weight: var(--mido-font-weight-bold);
  letter-spacing: 1px;
}

.login__brand-hero {
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-4);
}

.login__brand-title {
  margin: 0;
  /* 登录品牌页营销主标，超出数据密度字号体系，故就地定义并注释 */
  font-size: 40px;
  line-height: 1.3;
  font-weight: var(--mido-font-weight-bold);
}

.login__brand-sub {
  margin: 0;
  font-size: var(--mido-font-size-body);
  line-height: var(--mido-line-height-body);
  color: var(--mido-nav-text);
}

.login__brand-foot {
  margin: 0;
  font-size: var(--mido-font-size-caption);
  line-height: var(--mido-line-height-caption);
  color: color-mix(in srgb, var(--mido-nav-text) 70%, transparent);
}

/* ===== 登录区 ===== */
.login__form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--mido-space-6);
  background-color: var(--el-bg-color);
}

.login__card {
  width: var(--mido-login-card-width);
  border: none;
  padding: var(--mido-space-5) var(--mido-space-4);
}

.login__welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: var(--mido-space-6);
}

.login__welcome-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  /* 品牌图标徽章，尺寸为视觉装饰值，就地注释 */
  width: 48px;
  height: 48px;
  margin-bottom: var(--mido-space-3);
  border-radius: var(--mido-radius-lg);
  font-size: var(--mido-space-5);
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.login__welcome-title {
  margin: 0;
  font-size: var(--mido-font-size-h1);
  line-height: var(--mido-line-height-h1);
  font-weight: var(--mido-font-weight-bold);
  color: var(--el-text-color-primary);
}

.login__welcome-sub {
  margin: var(--mido-space-1) 0 0;
  font-size: var(--mido-font-size-secondary);
  line-height: var(--mido-line-height-secondary);
  color: var(--el-text-color-secondary);
}

.login__btn {
  width: 100%;
  margin-top: var(--mido-space-2);
  font-weight: var(--mido-font-weight-bold);
  letter-spacing: 2px;
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

/* ===== 响应式：按设计系统 §9 断点，<1280 隐藏品牌墙 ===== */
@media (max-width: 1279px) {
  .login {
    grid-template-columns: 1fr;
    background-color: var(--el-bg-color-page);
  }

  .login__brand-panel {
    display: none;
  }

  .login__card {
    box-shadow: var(--mido-shadow-card);
  }
}
</style>
