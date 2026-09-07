import http from './http'

/** 平台超管登录。对应 AdminAuthController#login */
export function login(payload) {
  return http.post('/admin/auth/login', payload)
}
