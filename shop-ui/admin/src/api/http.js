import axios from 'axios'
import { message } from 'ant-design-vue'

/**
 * HTTP 客户端。
 *
 * 后端统一返回 Result 结构：{ code, msg, data }，code=0 表示成功（见 shop-common 的 Result/ErrorCode）。
 * 因此这里做两层解包：
 *   1. HTTP 层非 2xx（401/500 等）→ 走 error 分支
 *   2. HTTP 200 但 code !== 0 → 也算业务失败，抛出去让调用方 catch，避免每个页面都写 if (res.code === 0)
 *
 * baseURL 刻意留空：前端全部用相对路径 /admin/**，开发时由 Vite proxy 转发，
 * 生产由 Nginx 反代（docker/nginx-admin.conf）。这样代码里没有环境判断，也不会有 CORS。
 */
const http = axios.create({
  timeout: 20000,
})

/** 业务错误码，与 shop-common 的 ErrorCode 对齐（只列前端需要分支处理的） */
export const ErrorCode = {
  OK: 0,
  PARAM_INVALID: 10001,
  UNAUTHORIZED: 10002,
  FORBIDDEN: 10003,
  NOT_FOUND: 10004,
  SYSTEM_ERROR: 10500,
}

const TOKEN_KEY = 'shop_admin_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** 由 router 注入，避免这里直接 import router 造成循环依赖 */
let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非 Result 结构（比如将来接文件下载）直接原样返回
    if (body === null || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (body.code === ErrorCode.OK) {
      return body.data
    }
    // 业务失败：统一提示 + 抛错，调用方只处理需要特殊分支的情况
    message.error(body.msg || '请求失败')
    return Promise.reject(Object.assign(new Error(body.msg || '请求失败'), { code: body.code, isBusiness: true }))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data

    // 401 是 AdminAuthFilter 拦断的信号：token 缺失/过期/或拿商户 token 打平台接口。
    // 这里清 token 并跳登录页，不额外弹提示（跳转本身已经是反馈）。
    if (status === 401) {
      clearToken()
      onUnauthorized()
      return Promise.reject(Object.assign(new Error(body?.msg || '登录已过期'), { code: body?.code, isAuth: true }))
    }

    const msg = body?.msg || (status ? `请求失败（HTTP ${status}）` : '网络异常，请检查连接')
    message.error(msg)
    return Promise.reject(error)
  },
)

export default http
