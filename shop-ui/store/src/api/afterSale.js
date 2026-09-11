import http from './http'

/** 商户售后管理。对应 StoreAfterSaleController（/store/after-sale）。 */
export function pageAfterSales(params) {
  return http.get('/store/after-sale', { params })
}

export function getAfterSale(id) {
  return http.get(`/store/after-sale/${id}`)
}

export function approveAfterSale(id, auditRemark, returnAddressId) {
  return http.post(`/store/after-sale/${id}/approve`, { auditRemark: auditRemark || null, returnAddressId: returnAddressId || null })
}

export function rejectAfterSale(id, auditRemark) {
  return http.post(`/store/after-sale/${id}/reject`, auditRemark ? { auditRemark } : null)
}

export function refundAfterSale(id) {
  return http.post(`/store/after-sale/${id}/refund`)
}
