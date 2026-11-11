import { get } from '@/utils/request'

const PLATFORM_AMAP_JS_KEY = '65eeae53a1f353c31e329e44c774ca7e'
const PLATFORM_AMAP_JS_SECRET = 'e773074f110402b905245336232c9ffb'

let cachedConfig = null
let loadingScript = null

export function lookupMapLocation(longitude, latitude) {
  return get('/api/map/lookup', { longitude, latitude })
}

export async function getMapConfig() {
  if (cachedConfig?.apiKey) return cachedConfig
  try {
    const remote = await get('/api/map/config')
    cachedConfig = {
      provider: remote?.provider || 'amap',
      apiKey: String(remote?.apiKey || '').trim() || PLATFORM_AMAP_JS_KEY,
      securityJsCode: String(remote?.securityJsCode || '').trim() || PLATFORM_AMAP_JS_SECRET,
    }
  } catch (e) {
    cachedConfig = {
      provider: 'amap',
      apiKey: PLATFORM_AMAP_JS_KEY,
      securityJsCode: PLATFORM_AMAP_JS_SECRET,
    }
  }
  return cachedConfig
}

export function loadAmapJs(key, securityJsCode) {
  if (typeof window === 'undefined') return Promise.reject(new Error('当前环境无法加载地图'))
  const secret = String(securityJsCode || PLATFORM_AMAP_JS_SECRET).trim()
  if (secret) {
    window._AMapSecurityConfig = {
      ...(window._AMapSecurityConfig || {}),
      securityJsCode: secret,
    }
  }
  window.forbidenWebGL = true
  if (window.AMap) return Promise.resolve(window.AMap)
  const jsKey = String(key || '').trim()
  if (!jsKey) return Promise.reject(new Error('未配置高德地图 Key'))
  if (loadingScript) return loadingScript
  loadingScript = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.charset = 'utf-8'
    script.async = false
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(jsKey)}`
    script.onload = () => {
      if (window.AMap) resolve(window.AMap)
      else {
        loadingScript = null
        reject(new Error('高德地图加载失败'))
      }
    }
    script.onerror = () => {
      loadingScript = null
      reject(new Error('高德地图加载失败，请检查 Web 端 Key 和安全密钥'))
    }
    document.head.appendChild(script)
  })
  return loadingScript
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

export function openMapNav({ latitude, longitude, name, address }) {
  const lat = Number(latitude)
  const lng = Number(longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
    uni.showToast({ title: '该地点尚未地图选点', icon: 'none' })
    return
  }
  const title = name || '目的地'
  // #ifdef H5
  const url = `https://uri.amap.com/navigation?to=${lng},${lat},${encodeURIComponent(title)}&mode=car&coordinate=gaode&callnative=1`
  window.open(url, '_blank')
  // #endif
  // #ifndef H5
  uni.openLocation({ latitude: lat, longitude: lng, name: title, address: address || '' })
  // #endif
}

export function haversineKm(lat1, lng1, lat2, lng2) {
  const toRad = (deg) => (deg * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

export function formatDistance(km) {
  if (!Number.isFinite(km)) return ''
  if (km < 1) return `${Math.max(1, Math.round(km * 1000))}m`
  return `${km.toFixed(km < 10 ? 1 : 0)}km`
}
