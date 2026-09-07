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
 *   - H5/公众号：不带 X-Shop-Id，让后端按 Host 反查 —— 这样同一份 H5 代码能服务所有租户，
 *     换域名即换商城；如果前端写死 shopId，就退化成"一个租户一份构建产物"了。
 * 识别失败后端会返回 20000（商城不存在），不要在前端静默重试。
 */

const TOKEN_KEY = 'shop_client_token'
const SHOP_ID_KEY = 'shop_client_shop_id'
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

export function request(options) {
  const { url, method = 'GET', data, header = {}, showLoading = false } = options

  const token = getToken()
  if (token) header.Authorization = `Bearer ${token}`

  // 只有小程序/APP 需要显式声明租户；H5 生产环境交给后端按 Host 反查。
  // #ifndef H5
  const shopId = getShopId()
  if (shopId) header['X-Shop-Id'] = shopId
  // #endif
  // #ifdef H5
  // 正式环境按访问域名反查租户，不带头。本机 / 局域网 IP 没有域名可反查
  // （测试机访问开发机 http://192.168.x.x:8092），必须带上手动指定的 shopId。
  if (h5NeedsManualShopId()) {
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
