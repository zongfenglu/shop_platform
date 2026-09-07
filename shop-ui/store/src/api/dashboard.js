import http from './http'

export function getStoreDashboard() {
  return http.get('/store/dashboard/overview')
}

export function getStoreDaily(params) {
  return http.get('/store/dashboard/daily', { params })
}
