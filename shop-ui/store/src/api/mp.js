import http from './http'

export function getStoreMp() {
  return http.get('/store/mp')
}

export function saveStoreMpSelf(payload) {
  return http.post('/store/mp/self', payload)
}

export function getStoreMpAuthUrl(appType) {
  return http.post('/store/mp/auth-url', null, { params: { appType } })
}

export function getStoreMpExtJson(appType) {
  return http.get('/store/mp/ext-json', { params: { appType } })
}

export function unbindStoreMp(appType) {
  return http.delete(`/store/mp/${appType}`)
}
