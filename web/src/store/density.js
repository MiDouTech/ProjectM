import { defineStore } from 'pinia'

const KEY = 'mido_density'

/**
 * 界面密度档（全局基座，从 ops 上移；design-system §3「密度档」）。
 * - comfortable：租户默认，留白引导（= 现有观感）。
 * - compact：数据密集视图（表格/列表/费用/报表），一屏信息最大化。
 * 持久化到 localStorage；MainLayout 据此注入根节点 data-density + el-config-provider size，
 * 组件按 --mido-density-* 取间距，页面不写死。ops 端固定 compact，不读本 store。
 */
export const useDensityStore = defineStore('density', {
  state: () => ({
    density: localStorage.getItem(KEY) === 'compact' ? 'compact' : 'comfortable',
  }),
  getters: {
    isCompact: (state) => state.density === 'compact',
    // 密度档联动 Element 组件尺寸：comfortable=default / compact=small
    elSize: (state) => (state.density === 'compact' ? 'small' : 'default'),
  },
  actions: {
    setDensity(v) {
      this.density = v === 'compact' ? 'compact' : 'comfortable'
      localStorage.setItem(KEY, this.density)
    },
    toggle() {
      this.setDensity(this.density === 'compact' ? 'comfortable' : 'compact')
    },
  },
})
