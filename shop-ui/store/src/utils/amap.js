export const PLATFORM_AMAP_JS_KEY = '65eeae53a1f353c31e329e44c774ca7e'
export const PLATFORM_AMAP_JS_SECRET = 'e773074f110402b905245336232c9ffb'

let loading = null

export function loadAmapJs(key, securityJsCode) {
  if (typeof window === 'undefined') return Promise.reject(new Error('当前环境无法加载地图'))
  if (window.AMap) return Promise.resolve(window.AMap)
  const jsKey = String(key || PLATFORM_AMAP_JS_KEY).trim()
  if (!jsKey) return Promise.reject(new Error('未配置高德地图 Key'))
  if (loading) return loading
  const secret = String(securityJsCode || PLATFORM_AMAP_JS_SECRET).trim()
  if (secret) {
    window._AMapSecurityConfig = { securityJsCode: secret }
  }
  loading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(jsKey)}`
    script.onload = () => {
      if (window.AMap) resolve(window.AMap)
      else {
        loading = null
        reject(new Error('高德地图加载失败'))
      }
    }
    script.onerror = () => {
      loading = null
      reject(new Error('高德地图加载失败，请检查 Key、安全密钥和域名白名单'))
    }
    document.head.appendChild(script)
  })
  return loading
}
