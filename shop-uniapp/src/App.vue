<script>
import { setH5PreviewShopId } from '@/utils/request.js'

export default {
  onLaunch() {
    // 小程序端：ext.json 注入的 shopId 在这里最早可读，request.js 会自动带上 X-Shop-Id

    // #ifdef H5
    // H5 共享预览入口：从 URL 参数读取 shopId，并保持在当前标签页内。
    this.setupH5ShopId()
    // #endif
  },
  methods: {
    // #ifdef H5
    setupH5ShopId() {
      try {
        const url = new URL(window.location.href)
        const urlShopId = url.searchParams.get('_shopId')
        if (urlShopId) {
          setH5PreviewShopId(urlShopId)
          console.log('[H5] shopId from URL:', urlShopId)
        }
      } catch (e) {
        console.warn('[H5] Failed to parse URL:', e)
      }

      window.addEventListener('message', (event) => {
        if (event.data && event.data.type === 'setShopId' && event.data.shopId) {
          setH5PreviewShopId(event.data.shopId)
          console.log('[H5] shopId from postMessage:', event.data.shopId)
        }
      })
    }
    // #endif
  }
}
</script>

<style>
/* 全局样式：token 与原型 docs/prototype/shared/style.css 保持一致 */
@import './styles/tokens.css';

page {
  background: #f9f9f7;
  color: #0b0b0b;
  font-size: 28rpx;
  font-family: system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
