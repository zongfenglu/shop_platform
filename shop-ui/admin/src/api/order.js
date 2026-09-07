import http from './http'

/**
 * 平台订单管理。
 */
export function pageShopOrders(params) {
  return http.get('/admin/orders', { params })
}

export function confirmShopOrderPaid(id) {
  return http.post(`/admin/orders/${id}/confirm-paid`)
}
