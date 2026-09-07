import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 开发时同源代理到 admin-api：前端一律用相对路径 /admin/**，
    // 生产环境由 Nginx 做同样的反代（docker/nginx-admin.conf），两边行为一致，
    // 因此代码里不需要区分环境的 baseURL，也没有 CORS 问题。
    proxy: {
      '/admin': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 生产构建产物目录，docker/Dockerfile.frontend 从这里取 dist/
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        // antd 整包约 1.5MB，不拆分会和业务代码打进同一个 chunk：
        // 每次改业务代码都让用户重新下载整个 UI 库，缓存完全失效。
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          antd: ['ant-design-vue'],
        },
      },
    },
  },
})
