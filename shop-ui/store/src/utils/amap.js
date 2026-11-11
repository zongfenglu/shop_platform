export const PLATFORM_AMAP_JS_KEY = '65eeae53a1f353c31e329e44c774ca7e'
export const PLATFORM_AMAP_JS_SECRET = 'e773074f110402b905245336232c9ffb'

let loading = null

export function applyAmapSecurity(securityJsCode) {
  const secret = String(securityJsCode || PLATFORM_AMAP_JS_SECRET).trim()
  if (secret) {
    window._AMapSecurityConfig = {
      ...(window._AMapSecurityConfig || {}),
      securityJsCode: secret,
    }
  }
  // 官方变量名就是 forbidenWebGL：禁用 WebGL 矢量底图，改用栅格瓦片。
  window.forbidenWebGL = true
}

export function loadAmapJs(key, securityJsCode) {
  if (typeof window === 'undefined') return Promise.reject(new Error('当前环境无法加载地图'))
  applyAmapSecurity(securityJsCode)
  if (window.AMap) return Promise.resolve(window.AMap)
  const jsKey = String(key || PLATFORM_AMAP_JS_KEY).trim()
  if (!jsKey) return Promise.reject(new Error('未配置高德地图 Key'))
  if (loading) return loading
  loading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.charset = 'utf-8'
    script.async = false
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
      reject(new Error('高德地图加载失败，请检查 Web 端 Key 和安全密钥'))
    }
    document.head.appendChild(script)
  })
  return loading
}

export function waitForElementSize(el, timeout = 2500) {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    const tick = () => {
      if (el && el.clientWidth > 8 && el.clientHeight > 8) {
        resolve(el)
        return
      }
      if (Date.now() - start > timeout) {
        reject(new Error('地图容器尚未展开，请关闭后重试'))
        return
      }
      requestAnimationFrame(tick)
    }
    tick()
  })
}
