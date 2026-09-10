/**
 * 统一请求封装（跨端：小程序 / H5 / APP）。
 *
 * 多租户识别是这里最要紧的事。后端 ClientTenantFilter 按三条来源识别租户（文档三 §2.2）：
 *   ① 请求头 X-Shop-Id —— 最明确，小程序由 ext.json 注入
 *   ② Host 反查域名表 —— H5 走租户自定义域名/泛域名
 *   ③ 小程序 AppID 反查 —— 兜底
 *
 * 因此各端策略不同：
 *   - 小程序：必须显式带 X-Shop-Id（小程序没有"域名"概念，Host 是我们的 API 域名，②③都不可靠）
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
 * 小程序端的 shopId。
 * 正式发布时由小程序 ext.json 注入（每个租户一个小程序，构建产物相同、ext 不同），
 * 开发阶段可用 setShopId() 手动指定，方便本地联调。
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
  if (!/^\d+$/.test(value)) return
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

  // 只有小程序/APP 需要显式声明租户；H5 生产环境交给后端按 Host 反查。
  // #ifndef H5
  const shopId = getShopId()
  if (shopId) header['X-Shop-Id'] = shopId
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
      data,
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
          resolve(body)
          return
        }
        if (body.code === ErrorCode.OK) {
          resolve(body.data)
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
