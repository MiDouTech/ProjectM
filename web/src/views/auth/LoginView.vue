<template>
  <div class="login">
    <!-- 品牌墙 -->
    <aside class="login__brand-panel">
      <!-- 背景照片层（蓝色星球）：半透明 + 左侧渐隐，清晰融入品牌墙不突兀。
           图片放 web/public/login-bg.jpg；缺失时优雅回落到下方纯 CSS 渐变 -->
      <div class="login__brand-photo" aria-hidden="true"></div>
      <!-- 半透明品牌背景：蓝图点阵网格 + 柔光斑（纯 CSS，由 token 派生）-->
      <div class="login__brand-grid" aria-hidden="true"></div>
      <div class="login__brand-deco" aria-hidden="true"></div>
      <div class="login__brand-glow" aria-hidden="true"></div>

      <div class="login__brand-content">
        <div class="login__brand-top">
          <el-icon class="login__brand-logo"><Grid /></el-icon>
          <span class="login__brand-name">米多项目管理</span>
        </div>

        <div class="login__brand-hero">
          <span class="login__brand-eyebrow">MIDO · PROJECT MANAGEMENT</span>
          <h2 class="login__brand-title">让每一个项目<br />有据可依 · 有值可验</h2>
          <p class="login__brand-sub">从立项到价值验收，把"项目成功"重新定义</p>
          <ul class="login__brand-feats">
            <li>
              <el-icon class="login__feat-icon"><Stamp /></el-icon>
              <span>立项审批引擎 · 严肃闸门</span>
            </li>
            <li>
              <el-icon class="login__feat-icon"><User /></el-icon>
              <span>干系人管理 · 权力利益对齐</span>
            </li>
            <li>
              <el-icon class="login__feat-icon"><TrendCharts /></el-icon>
              <span>NPSS 两段式价值验收</span>
            </li>
          </ul>
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
import { Grid, User, Lock, Stamp, TrendCharts } from '@element-plus/icons-vue'
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
    var(--el-color-primary-dark-2) 58%,
    var(--mido-cat-s) 135%
  );
  color: var(--mido-nav-text-active);
}

/* 背景照片层：蓝色星球。cover 充满，半透明融入渐变；左侧用 mask 渐隐，保证标题区干净不突兀 */
.login__brand-photo {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: url('/login-bg.jpg');
  background-size: cover;
  background-position: right center;
  /* 半透明：星球清晰可辨又与品牌渐变融合 */
  opacity: 0.62;
  /* 左→右渐显：标题/卖点所在的左半区近乎隐去，星球留在右侧空白区，过渡柔和不突兀 */
  -webkit-mask-image: linear-gradient(
    100deg, transparent 0%, var(--mido-nav-text-active) 52%, var(--mido-nav-text-active) 100%);
  mask-image: linear-gradient(
    100deg, transparent 0%, var(--mido-nav-text-active) 52%, var(--mido-nav-text-active) 100%);
}

/* 蓝图点阵网格：半透明背景纹理，由品牌色派生，左上浓右下淡 */
.login__brand-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  /* 点阵密度为视觉装饰值，就地定义并注释 */
  background-image: radial-gradient(
    color-mix(in srgb, var(--mido-nav-text-active) 16%, transparent) 1px,
    transparent 1px
  );
  background-size: 22px 22px;
  /* mask 走 alpha 通道：恒不透明的 token 色 → transparent，渐隐效果，不写裸 hex */
  -webkit-mask-image: linear-gradient(135deg, var(--mido-nav-text-active) 0%, transparent 72%);
  mask-image: linear-gradient(135deg, var(--mido-nav-text-active) 0%, transparent 72%);
  opacity: 0.7;
}

/* 柔光装饰圆斑：用 color-mix 从 token 派生半透明，不写裸 rgba */
.login__brand-deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      42% 42% at 80% 16%,
      color-mix(in srgb, var(--mido-cat-i) 38%, transparent) 0%,
      transparent 70%
    ),
    radial-gradient(
      40% 40% at 10% 90%,
      color-mix(in srgb, var(--mido-cat-s) 55%, transparent) 0%,
      transparent 70%
    );
}

/* 顶部高光晕：增强景深，半透明 */
.login__brand-glow {
  position: absolute;
  /* 大尺寸柔光环为装饰值，就地注释 */
  top: -180px;
  right: -120px;
  width: 460px;
  height: 460px;
  pointer-events: none;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--mido-nav-text-active) 14%, transparent) 0%,
    transparent 65%
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
  animation: brand-rise 0.6s ease both;
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

.login__brand-eyebrow {
  font-size: var(--mido-font-size-caption);
  line-height: var(--mido-line-height-caption);
  font-weight: var(--mido-font-weight-bold);
  /* 字母间距为装饰值 */
  letter-spacing: 3px;
  color: color-mix(in srgb, var(--mido-nav-text-active) 72%, transparent);
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

.login__brand-feats {
  list-style: none;
  margin: var(--mido-space-2) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--mido-space-3);
}

.login__brand-feats li {
  display: flex;
  align-items: center;
  gap: var(--mido-space-3);
  font-size: var(--mido-font-size-body);
  line-height: var(--mido-line-height-body);
  color: color-mix(in srgb, var(--mido-nav-text-active) 90%, transparent);
}

.login__feat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  /* 卖点图标徽章尺寸，装饰值就地注释 */
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  border-radius: var(--mido-radius-md);
  font-size: var(--mido-font-size-secondary);
  color: var(--mido-nav-text-active);
  background-color: color-mix(in srgb, var(--mido-nav-text-active) 14%, transparent);
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
  animation: card-rise 0.5s ease both;
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

@keyframes brand-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes card-rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login__brand-content,
  .login__card {
    animation: none;
  }
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
