import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5175,
    // H5 开发时同源代理到 client-api，避免跨域；
    // 小程序端不走这个代理（小程序用 request 直连配置的域名），见 src/utils/request.js
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
})
