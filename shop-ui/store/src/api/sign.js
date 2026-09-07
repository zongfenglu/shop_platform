import http from './http'

/** 获取签到配置 */
export function getSignConfig() {
  return http.get('/store/sign-config')
}

/** 保存签到配置（覆盖式） */
export function saveSignConfig(payload) {
  return http.post('/store/sign-config', payload)
}
