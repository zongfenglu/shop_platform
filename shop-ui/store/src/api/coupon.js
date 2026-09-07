import http from './http'

export function listCoupons() {
  return http.get('/store/coupons')
}

export function createCoupon(payload) {
  return http.post('/store/coupons', payload)
}

export function updateCoupon(id, payload) {
  return http.put(`/store/coupons/${id}`, payload)
}

export function deleteCoupon(id) {
  return http.delete(`/store/coupons/${id}`)
}
