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

function loadPlugins(names) {
  return new Promise((resolve, reject) => {
    if (!window.AMap?.plugin) {
      reject(new Error('高德地图插件不可用'))
      return
    }
    window.AMap.plugin(names, () => resolve(window.AMap))
  })
}

function toLngLat(loc, longitude, latitude) {
  if (loc && typeof loc.getLng === 'function') {
    return { longitude: loc.getLng(), latitude: loc.getLat() }
  }
  if (Array.isArray(loc) && loc.length >= 2) {
    return { longitude: Number(loc[0]), latitude: Number(loc[1]) }
  }
  if (typeof loc === 'string' && loc.includes(',')) {
    const [lng, lat] = loc.split(',')
    return { longitude: Number(lng), latitude: Number(lat) }
  }
  if (loc && loc.lng != null) {
    return { longitude: Number(loc.lng), latitude: Number(loc.lat) }
  }
  return { longitude, latitude }
}

function textCity(city, province) {
  if (city == null || city === '' || Array.isArray(city)) return province || ''
  return String(city)
}

function streetOf(ac) {
  if (!ac) return ''
  const streetNumber = ac.streetNumber
  if (streetNumber && typeof streetNumber === 'object') {
    return `${ac.street || streetNumber.street || ''}${streetNumber.number || ''}`.trim()
  }
  return `${ac.street || ''}${streetNumber || ''}`.trim()
}

function stripRegion(detail, province, city, district) {
  let value = String(detail || '').trim()
  for (const part of [province, city, district]) {
    if (part && value.startsWith(part)) value = value.slice(part.length).trim()
  }
  return value
}

function mapPois(list, longitude, latitude, formatted) {
  return (Array.isArray(list) ? list : [])
    .map((poi) => {
      const pos = toLngLat(poi?.location, longitude, latitude)
      return {
        name: String(poi?.name || '').trim(),
        address: String(poi?.address || formatted || '').trim(),
        distance: poi?.distance != null && poi.distance !== '' ? String(poi.distance) : '',
        longitude: pos.longitude,
        latitude: pos.latitude,
      }
    })
    .filter((poi) => poi.name)
    .slice(0, 12)
}

function fromRegeo(regeo, longitude, latitude) {
  const ac = regeo?.addressComponent || {}
  const formatted = regeo?.formattedAddress || regeo?.formatted_address || ''
  const pois = mapPois(regeo?.pois, longitude, latitude, formatted)
  const province = ac.province || ''
  const city = textCity(ac.city, province)
  const district = ac.district || ''
  const street = streetOf(ac)
  const township = ac.township || ''
  const name = pois[0]?.name || street || township || district || '已选位置'
  const detail = [street, pois[0]?.name && pois[0].name !== street ? pois[0].name : '']
    .filter(Boolean)
    .join(' ') || township || formatted
  return {
    name,
    address: formatted,
    detail: stripRegion(detail, province, city, district),
    province,
    city,
    district,
    region: district,
    pois,
  }
}

function searchNearbyPois(longitude, latitude) {
  return new Promise((resolve) => {
    const placeSearch = new window.AMap.PlaceSearch({
      pageSize: 12,
      pageIndex: 1,
      city: '全国',
      extensions: 'all',
    })
    placeSearch.searchNearBy('小区', [longitude, latitude], 800, (status, result) => {
      if (status === 'complete' && result?.poiList?.pois?.length) {
        resolve(mapPois(result.poiList.pois, longitude, latitude, ''))
        return
      }
      resolve([])
    })
  })
}

/** 用 JS API 做逆地理 + 附近 POI，不依赖 Web 服务 Key 的数字签名。 */
export async function reverseGeocode(longitude, latitude) {
  if (!window.AMap) return null
  await loadPlugins(['AMap.Geocoder', 'AMap.PlaceSearch'])
  const regeo = await new Promise((resolve) => {
    const geocoder = new window.AMap.Geocoder({
      city: '全国',
      radius: 1000,
      extensions: 'all',
    })
    geocoder.getAddress([longitude, latitude], (status, result) => {
      if (status === 'complete' && result?.regeocode) resolve(result.regeocode)
      else resolve(null)
    })
  })
  let data = regeo ? fromRegeo(regeo, longitude, latitude) : null
  if (!data?.pois?.length) {
    const nearby = await searchNearbyPois(longitude, latitude)
    if (nearby.length) {
      data = {
        name: data?.name || nearby[0].name,
        address: data?.address || nearby[0].address,
        detail: data?.detail || nearby[0].name,
        province: data?.province || '',
        city: data?.city || '',
        district: data?.district || '',
        region: data?.region || '',
        pois: nearby,
      }
    }
  }
  return data
}
