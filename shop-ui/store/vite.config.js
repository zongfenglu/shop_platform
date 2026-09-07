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
    // 与 admin（5173）错开，两个后台可以同时开着调试
    port: 5174,
    proxy: {
      '/store': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8082',
        changeOrigin: true,
      },
      // 上传的图片由 store-api 直接以静态资源提供，dev 下也要转发过去
      '/uploads': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          antd: ['ant-design-vue'],
        },
      },
    },
  },
})
