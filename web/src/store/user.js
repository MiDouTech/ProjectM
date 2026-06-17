import { defineStore } from 'pinia'

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
  }),
  getters: {
    isLogin: (state) => !!state.token,
    // 当前登录用户 ID（从 JWT subject 解析）
    userId: (state) => {
      const claims = state.token ? decodeJwt(state.token) : null
      return claims?.sub ? Number(claims.sub) : null
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
    },
  },
})

export { TOKEN_KEY }
