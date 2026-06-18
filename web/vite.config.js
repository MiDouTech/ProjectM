import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器把 /api 代理到后端 Spring Boot（localhost:8080）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      // frappe-gantt 的 exports 仅暴露入口，CSS 需显式指向 dist 文件
      'frappe-gantt/dist/frappe-gantt.css': fileURLToPath(
        new URL('./node_modules/frappe-gantt/dist/frappe-gantt.css', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        // 拆分大体积第三方库：AntV 抽成共享 chunk（Goal/Report 去重 + 路由懒加载按需），
        // 甘特图独立；其余第三方（Vue 全家桶 + Element Plus）合并为 vendor，避免循环 chunk。
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('@antv')) return 'antv'
          if (id.includes('frappe-gantt')) return 'gantt'
          return 'vendor'
        },
      },
    },
  },
})
