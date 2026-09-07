import http from './http'

/** 商户订单管理。对应 StoreOrderController（/store/order）。 */
export function pageOrders(params) {
  return http.get('/store/order', { params })
}

export function getOrder(id) {
  return http.get(`/store/order/${id}`)
}

export function shipOrder(id, payload) {
  return http.post(`/store/order/${id}/ship`, payload)
}

export function cancelOrder(id, reason) {
  return http.post(`/store/order/${id}/cancel`, null, { params: reason ? { reason } : {} })
}
