import { defineStore } from 'pinia'
import { appApi } from '@/api/app'

const TOKEN_KEY = 'mido_token'

/**
 * 用户/认证状态。JWT 持久化到 localStorage，Axios 请求拦截器据此注入。
 * 登录逻辑随 Step 1（module-org 认证）接入，这里先提供 token 读写骨架。
 */
/** 解析 JWT payload（base64url），取不到返回 null。后端 subject=userId（无 /me 接口）。 */
function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    // 当前租户启用的功能码（功能门控用）。空数组语义为「未取到」，
    // 由 hasFeature 走 fail-open 默认全显示，避免误隐藏。
    features: [],
    featuresLoaded: false,
  }),
  getters: {
    isLogin: (state) => !!state.token,
    // 当前登录用户 ID（从 JWT subject 解析）。雪花 ID 为 19 位，
    // 禁止 Number() 转换（会丢精度），保持字符串透传给后端。
    userId: (state) => {
      const claims = state.token ? decodeJwt(state.token) : null
      return claims?.sub ? String(claims.sub) : null
    },
    // 功能门控：未取到功能列表（featuresLoaded=false 或空）时默认放行（fail-open）。
    hasFeature: (state) => (code) => {
      if (!state.featuresLoaded || !state.features.length) return true
      return state.features.includes(code)
    },
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    clearToken() {
      this.token = ''
      localStorage.removeItem(TOKEN_KEY)
      this.features = []
      this.featuresLoaded = false
    },
    // 拉取当前租户启用的功能码（失败保持空数组 → fail-open）
    async fetchFeatures() {
      try {
        const list = await appApi.features()
        this.features = Array.isArray(list) ? list : []
        this.featuresLoaded = true
      } catch {
        this.features = []
        this.featuresLoaded = false
      }
    },
  },
})

export { TOKEN_KEY }
