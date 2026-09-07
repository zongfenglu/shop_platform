import http from './http'

export function listOfflineStores() {
  return http.get('/store/offline-stores')
}

export function createOfflineStore(payload) {
  return http.post('/store/offline-stores', payload)
}

export function updateOfflineStore(id, payload) {
  return http.put(`/store/offline-stores/${id}`, payload)
}

export function updateOfflineStoreStatus(id, status) {
  return http.put(`/store/offline-stores/${id}/status`, { status })
}

export function listVerifyLogs() {
  return http.get('/store/offline/verify-logs')
}

export function verifyPickupCode(verifyCode) {
  return http.post('/store/offline/verify', { verifyCode })
}
