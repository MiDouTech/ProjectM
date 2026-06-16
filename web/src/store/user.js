import { defineStore } from 'pinia'

const TOKEN_KEY = 'mido_token'

/**
 * 用户/认证状态。JWT 持久化到 localStorage，Axios 请求拦截器据此注入。
 * 登录逻辑随 Step 1（module-org 认证）接入，这里先提供 token 读写骨架。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
  }),
  getters: {
    isLogin: (state) => !!state.token,
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
