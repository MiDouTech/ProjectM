<template>
  <div class="login">
    <!-- 全屏底图（蓝色星球）+ 品牌氛围层：单一整图，无左右分栏 -->
    <div class="login__bg" aria-hidden="true"></div>
    <div class="login__scrim" aria-hidden="true"></div>
    <div class="login__grid" aria-hidden="true"></div>
    <div class="login__glow" aria-hidden="true"></div>

    <!-- 正中登录区 -->
    <main class="login__stage">
      <section class="login__card">
        <header class="login__head">
          <!-- 米多竖版 logo（缺失时优雅回落到图标+名，便于直接放图即生效） -->
          <img
            v-if="logoOk"
            class="login__logo"
            src="/logo_竖_蓝色.png"
            alt="米多 · 通用项目管理系统"
            @error="logoOk = false"
          />
          <div v-else class="login__logo-fallback">
            <el-icon><Grid /></el-icon>
            <span>米多</span>
          </div>
          <h1 class="login__title">欢迎回来</h1>
          <p class="login__sub">登录米多 · 通用项目管理系统</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="手机号 / 用户名"
              aria-label="手机号或用户名"
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
          <span class="login__feat">立项审批引擎</span>
          <i class="login__sep" aria-hidden="true"></i>
          <span class="login__feat">干系人管理</span>
          <i class="login__sep" aria-hidden="true"></i>
          <span class="login__feat">NPSS 价值验收</span>
        </footer>
      </section>

      <p class="login__copyright">© 2026 米多 · 通用项目管理系统</p>
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
const logoOk = ref(true)
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
  position: relative;
  min-height: 100dvh;
  height: 100%;
  overflow: hidden;
  background-color: var(--mido-nav-bg);
}

/* ===== 全屏底图 + 氛围层 ===== */
/* 底图：蓝色星球铺满；缺失时下方 scrim 的品牌渐变即为优雅回落 */
.login__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: url('/login-bg.jpg');
  background-size: cover;
  background-position: center;
}

/* 品牌罩：四周渐暗的暗角(让中央卡片更聚焦) + 斜向品牌色叠加(海军蓝→主蓝→战略紫)，
   让整图统一在品牌氛围里，又保留星球可见。透明度由 token 经 color-mix 派生，不写裸 rgba。 */
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

/* 蓝图点阵纹理：极淡，向中心聚拢渐隐，增加高级质感而不抢主体 */
.login__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  /* 点阵密度为装饰值，就地注释 */
  background-image: radial-gradient(
    color-mix(in srgb, var(--mido-nav-text-active) 10%, transparent) 1px,
    transparent 1px
  );
  background-size: 24px 24px;
  -webkit-mask-image: radial-gradient(circle at 50% 44%, var(--mido-nav-text-active) 0%, transparent 62%);
  mask-image: radial-gradient(circle at 50% 44%, var(--mido-nav-text-active) 0%, transparent 62%);
  opacity: 0.5;
}

/* 卡片背后的柔光晕：提升景深，让玻璃卡片"浮"起来 */
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

/* ===== 正中舞台 ===== */
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

/* 玻璃拟态卡片：近实心浅色面 + 背景模糊，置于深色底图上更显高端；
   无 backdrop-filter 的浏览器回落为高不透明浅面，文字对比依然达标。 */
.login__card {
  width: var(--mido-login-card-width);
  max-width: calc(100vw - var(--mido-space-6) * 2);
  padding: var(--mido-space-6) var(--mido-space-5) var(--mido-space-5);
  background-color: color-mix(in srgb, var(--el-bg-color) 90%, transparent);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  backdrop-filter: blur(20px) saturate(1.4);
  /* 大圆角/玻璃描边/深度浮层阴影均为高端质感装饰值，就地注释 */
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

/* 竖版 logo（1:1 含 midoo 字标）视觉高度为装饰值，就地注释 */
.login__logo {
  width: auto;
  height: 96px;
  margin-bottom: var(--mido-space-4);
  object-fit: contain;
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

/* 价值锚点：一行三卖点，弱化为脚注质感，呼应"立项→干系人→验收"主张 */
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

/* 卖点分隔点为装饰值 */
.login__sep {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background-color: var(--el-border-color);
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

/* 小屏：卡片自适应收窄（max-width 已处理），收紧留白 */
@media (max-width: 480px) {
  .login__stage {
    padding: var(--mido-space-4);
  }

  .login__card {
    padding: var(--mido-space-5) var(--mido-space-4) var(--mido-space-4);
  }
}
</style>
