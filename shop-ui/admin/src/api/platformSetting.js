import http from './http'

export function getPlatformSettings() {
  return http.get('/admin/settings')
}

export function savePlatformSetting(key, payload) {
  return http.put(`/admin/settings/${key}`, payload)
}
