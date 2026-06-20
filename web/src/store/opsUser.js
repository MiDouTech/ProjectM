import { defineStore } from 'pinia'
import { opsAuthApi } from '@/api/ops'

const OPS_TOKEN_KEY = 'mido_ops_token'

/**
 * 平台运营后台认证状态。与租户应用 useUserStore 完全隔离：
 * 独立 store id、独立 localStorage key（mido_ops_token），
 * 避免两套登录态互相覆盖。JWT 由 api/request.js 按路径前缀选择注入。
 */
export const useOpsUserStore = defineStore('opsUser', {
  state: () => ({
    token: localStorage.getItem(OPS_TOKEN_KEY) || '',
    // 当前运营账号信息（含 perms），登录后经 fetchMe 拉取
    me: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
    // 运营账号显示名（顶栏用），缺失时回落登录名
    displayName: (state) => state.me?.name || state.me?.username || '运营员',
    perms: (state) => state.me?.perms || [],
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem(OPS_TOKEN_KEY, token)
    },
    clearToken() {
      this.token = ''
      this.me = null
      localStorage.removeItem(OPS_TOKEN_KEY)
    },
    async fetchMe() {
      this.me = await opsAuthApi.me()
      return this.me
    },
  },
})

export { OPS_TOKEN_KEY }
