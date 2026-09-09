import http from './http'

/**
 * 商城（租户）管理。对应 AdminShopController。
 * 注意后端分页返回的是 MyBatis-Plus 的 IPage 结构：{ records, total, size, current, pages }。
 */
export function pageShops(params) {
  return http.get('/admin/shops', { params })
}

export function getShop(id) {
  return http.get(`/admin/shops/${id}`)
}

export function createShop(payload) {
  return http.post('/admin/shops', payload)
}

export function updateShop(id, payload) {
  return http.patch(`/admin/shops/${id}`, payload)
}

export function impersonateShop(id) {
  return http.post(`/admin/shops/${id}/impersonate`)
}

export function resetShopOwnerPassword(id, password) {
  return http.post(`/admin/shops/${id}/reset-password`, { password })
}

export function disableShop(id, reason) {
  return http.post(`/admin/shops/${id}/disable`, { reason })
}

export function enableShop(id) {
  return http.post(`/admin/shops/${id}/enable`)
}
