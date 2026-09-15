/**
 * 统一请求封装（跨端：小程序 / H5 / APP）。
 *
 * 多租户识别是这里最要紧的事。后端 ClientTenantFilter 按三条来源识别租户（文档三 §2.2）：
 *   ① 请求头 X-Shop-Id —— 最明确，小程序由 ext.json 注入
 *   ② Host 反查域名表 —— H5 走租户自定义域名/泛域名
 *   ③ 小程序 AppID 反查 —— 兜底
 *
 * 因此各端策略不同：
 *   - 小程序：启动后读取当前 AppID，请求带 X-Mini-AppId。后端按商户「设置 → 小程序设置」
 *     绑定的 AppID 定位店铺，再拉该店装修/商品。不要在代码里写死 shopId。
 *     未配置真实 AppID 时（游客号），才用启动参数 `_shopId` 做本地预览。
 *   - H5/公众号：普通访问不带 X-Shop-Id，让后端按 Host 反查；共享 H5 域名上的
 *     管理后台预览链接通过 _shopId 指定租户，并在当前标签页内携带 X-Shop-Id。
 * 识别失败后端会返回 20000（商城不存在），不要在前端静默重试。
 */

const TOKEN_KEY = 'shop_client_token'
const SHOP_ID_KEY = 'shop_client_shop_id'
const H5_PREVIEW_SHOP_ID_KEY = 'shop_client_preview_shop_id'
const USER_KEY = 'shop_client_user'

/** 业务错误码，与 shop-common 的 ErrorCode 对齐 */
export const ErrorCode = {
  OK: 0,
  UNAUTHORIZED: 10002,
  TENANT_NOT_FOUND: 20000,
}

export function getToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

/** 登录后存的最小用户信息（userId/nickname），登录响应里没有更多字段可存——
 *  会员等级/余额/积分/收藏等都要等 Sprint 7 会员体系落地才有对应接口。 */
export function getLoginUser() {
  try {
    const raw = uni.getStorageSync(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setLoginUser(user) {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

/**
 * 当前小程序 AppID。平台在商户后台绑定 AppID 后，后端可据此反查 shopId。
 * 游客号 touristappid 不算正式绑定，不传。
 */
export function getMiniAppId() {
  // #ifdef MP-WEIXIN
  try {
    const info = typeof uni.getAccountInfoSync === 'function' ? uni.getAccountInfoSync() : null
    const appId = info && info.miniProgram && info.miniProgram.appId
    if (appId && appId !== 'touristappid') return String(appId)
  } catch (e) {
    // 开发者工具未选 AppID 时可能拿不到
  }
  // #endif
  return ''
}

/**
 * 小程序端的 shopId。
 * 正式发布由平台下发的 ext.json 注入（构建产物相同、每个商户 ext 不同）。
 * 本地开发者工具没有 ext / 未配 AppID 时，才允许 setShopId() 临时预览某家店。
 */
export function getShopId() {
  // #ifdef MP
  try {
    const ext = uni.getExtConfigSync ? uni.getExtConfigSync() : null
    if (ext && ext.shopId) return String(ext.shopId)
  } catch (e) {
    // getExtConfigSync 在非第三方平台托管的小程序里不可用，属正常情况
  }
  // #endif
  try {
    return uni.getStorageSync(SHOP_ID_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setShopId(shopId) {
  uni.setStorageSync(SHOP_ID_KEY, String(shopId))
}

/** 是否为合法店铺数字 ID（H5 / 小程序启动参数 `_shopId`）。 */
export function isShopIdValue(value) {
  return /^\d+$/.test(String(value || '').trim())
}

/**
 * 和 H5 的 ?_shopId= 一样：从启动参数 / 页面参数里切店。
 * 换店时清掉上一店的登录态。有 ext.json 的正式小程序仍以 ext 为准。
 */
export function applyQueryShopId(query) {
  if (!query || typeof query !== 'object') return ''
  const raw = String(query._shopId || query.shopId || '').trim()
  if (!isShopIdValue(raw)) return ''
  try {
    const ext = uni.getExtConfigSync ? uni.getExtConfigSync() : null
    if (ext && ext.shopId) return String(ext.shopId)
  } catch (e) {
    // 非托管小程序没有 ext
  }
  const prev = (() => {
    try {
      return String(uni.getStorageSync(SHOP_ID_KEY) || '')
    } catch (e) {
      return ''
    }
  })()
  if (prev && prev !== raw) clearToken()
  setShopId(raw)
  return raw
}

/**
 * 返回当前 H5 标签页的预览商城。sessionStorage 只在当前标签页生效，避免访问
 * 正式租户域名时被之前预览过的商城 ID 污染。
 */
export function getH5PreviewShopId() {
  // #ifdef H5
  try {
    const fromUrl = new URL(window.location.href).searchParams.get('_shopId')
    if (fromUrl && /^\d+$/.test(fromUrl)) {
      window.sessionStorage.setItem(H5_PREVIEW_SHOP_ID_KEY, fromUrl)
      return fromUrl
    }
    const fromSession = window.sessionStorage.getItem(H5_PREVIEW_SHOP_ID_KEY) || ''
    return /^\d+$/.test(fromSession) ? fromSession : ''
  } catch (e) {
    return ''
  }
  // #endif
  // #ifndef H5
  return ''
  // #endif
}

export function setH5PreviewShopId(shopId) {
  const value = String(shopId || '').trim()
  if (!isShopIdValue(value)) return
  // #ifdef H5
  try {
    window.sessionStorage.setItem(H5_PREVIEW_SHOP_ID_KEY, value)
  } catch (e) {
    // sessionStorage 不可用时仍保留普通 shopId，至少保证首屏 URL 请求可识别租户
  }
  // #endif
  setShopId(value)
}

/**
 * API 基地址。
 * H5 用相对路径（开发走 Vite proxy、生产走 Nginx 反代，同源无跨域）；
 * 小程序必须是完整域名，且要在微信后台配置为 request 合法域名。
 */
function h5NeedsManualShopId() {
  if (import.meta.env.DEV) return true
  try {
    const host = typeof location !== 'undefined' ? location.hostname : ''
    if (!host || host === 'localhost' || host === '127.0.0.1') return true
    return /^(\d{1,3}\.){3}\d{1,3}$/.test(host)
  } catch (e) {
    return false
  }
}

function baseUrl() {
  // #ifdef H5
  return ''
  // #endif
  // #ifndef H5
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083'
  // #endif
}

/**
 * 把相对路径 /uploads/... 补全为可访问的完整 URL。
 * H5 端 nginx 代理了 /uploads/，直接用相对路径即可；
 * 小程序/APP 端无域名，必须带上 baseUrl。
 * 已经是 http(s):// 的外链不做处理直接返回。
 */
export function mediaUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  // #ifdef H5
  return url
  // #endif
  // #ifndef H5
  return (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083') + url
  // #endif
}

/**
 * 后端上传文件统一返回 /uploads/... 相对路径。浏览器能按当前站点解析，
 * 但小程序会把它当成本地包路径，因此在请求出口统一补全资源域名。
 */
export function normalizeMediaUrls(value) {
  if (typeof value === 'string') {
    return value.startsWith('/uploads/') ? mediaUrl(value) : value
  }
  if (Array.isArray(value)) {
    return value.map(normalizeMediaUrls)
  }
  if (value && typeof value === 'object') {
    Object.keys(value).forEach((key) => {
      value[key] = normalizeMediaUrls(value[key])
    })
  }
  return value
}

/**
 * uni.request 在小程序端会把对象里的 undefined 序列化成字面量 "undefined"。
 * GET 可选参数必须在发出前删除，否则后端数字参数会发生类型转换异常。
 */
export function compactRequestData(value) {
  if (Array.isArray(value)) {
    return value.map(compactRequestData)
  }
  if (value && Object.prototype.toString.call(value) === '[object Object]') {
    return Object.entries(value).reduce((result, [key, item]) => {
      if (item !== undefined) result[key] = compactRequestData(item)
      return result
    }, {})
  }
  return value
}

export function request(options) {
  const { url, method = 'GET', data, header = {}, showLoading = false } = options

  // #ifdef H5
  const previewShopId = getH5PreviewShopId()
  if (previewShopId && previewShopId !== getShopId()) {
    // h5.2doo.cn 是多商城共享入口，切换预览商城时不能沿用上一商城的用户登录态。
    clearToken()
    setShopId(previewShopId)
  }
  // #endif

  const token = getToken()
  if (token) header.Authorization = `Bearer ${token}`

  // 小程序：有真实 AppID 时只传 X-Mini-AppId，由后端按绑定表定位店铺。
  // 游客号 / 未选 AppID 时才传 X-Shop-Id（本地 _shopId 预览）。
  // #ifndef H5
  const miniAppId = getMiniAppId()
  if (miniAppId) {
    header['X-Mini-AppId'] = miniAppId
  } else {
    const shopId = getShopId()
    if (shopId) header['X-Shop-Id'] = shopId
  }
  // #endif
  // #ifdef H5
  // 管理后台预览使用共享 H5 域名，必须显式传 _shopId 对应的请求头；普通正式访问
  // 仍按域名反查。本机 / 局域网 IP 没有域名可反查，也使用手动指定的 shopId。
  if (previewShopId) {
    header['X-Shop-Id'] = previewShopId
  } else if (h5NeedsManualShopId()) {
    const manualShopId = getShopId()
    if (manualShopId) header['X-Shop-Id'] = manualShopId
  }
  // #endif

  if (showLoading) uni.showLoading({ title: '加载中', mask: true })

  return new Promise((resolve, reject) => {
    uni.request({
      url: baseUrl() + url,
      method,
      data: compactRequestData(data),
      header,
      timeout: 20000,
      success(res) {
        const body = res.data
        if (res.statusCode === 401) {
          clearToken()
          redirectToLogin()
          reject(new Error(body?.msg || '登录已过期'))
          return
        }
        if (!body || typeof body !== 'object' || !('code' in body)) {
          resolve(normalizeMediaUrls(body))
          return
        }
        if (body.code === ErrorCode.OK) {
          resolve(normalizeMediaUrls(body.data))
          return
        }
        // 登录态失效：/api/** 走 ClientTenantFilter（只识别不拦断），
        // 业务层抛 BusinessException → HTTP 200 + code=10002，必须按业务码判断
        if (body.code === ErrorCode.UNAUTHORIZED) {
          clearToken()
          redirectToLogin()
          reject(new Error(body.msg || '登录已过期'))
          return
        }
        uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
        reject(Object.assign(new Error(body.msg || '请求失败'), { code: body.code }))
      },
      fail(err) {
        uni.showToast({ title: '网络异常，请检查连接', icon: 'none' })
        reject(err)
      },
      complete() {
        if (showLoading) uni.hideLoading()
      },
    })
  })
}

function redirectToLogin() {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  if (current && current.route === 'pages/my/login') return
  uni.navigateTo({ url: '/pages/my/login' })
}

export const get = (url, data, opts) => request({ url, method: 'GET', data, ...opts })
export const post = (url, data, opts) => request({ url, method: 'POST', data, ...opts })
export const put = (url, data, opts) => request({ url, method: 'PUT', data, ...opts })
export const del = (url, data, opts) => request({ url, method: 'DELETE', data, ...opts })
