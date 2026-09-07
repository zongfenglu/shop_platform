import http from './http'

/** 商户后台登录。对应 StoreAuthController#login，需要 shopCode 定位租户 */
export function login(payload) {
  return http.post('/store/auth/login', payload)
}

export function impersonate(ticket) {
  return http.post('/store/auth/impersonate', { ticket })
}
