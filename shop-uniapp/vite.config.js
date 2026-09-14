import { copyFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/** 商户后台下载的 ext.json 放到项目根，编译进小程序目录，运行时才能读到该店 shopId。 */
function copyExtJson() {
  return {
    name: 'copy-mp-ext-json',
    writeBundle(options) {
      const src = resolve(__dirname, 'ext.json')
      if (!existsSync(src) || !options.dir) return
      copyFileSync(src, resolve(options.dir, 'ext.json'))
    },
  }
}

export default defineConfig({
  plugins: [uni(), copyExtJson()],
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
