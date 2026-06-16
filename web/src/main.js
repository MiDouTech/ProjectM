import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 样式顺序要点：先 Element Plus 默认皮肤，再用 tokens.css 覆盖主题，最后全局基础样式。
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import './styles/global.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 注册全部 Element Plus 图标（导航等占位使用）
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
