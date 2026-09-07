import http from './http'

// ---- 分销设置 ----
export function getDealerSettings() {
  return http.get('/store/dealer/settings')
}

export function saveDealerSettings(payload) {
  return http.post('/store/dealer/settings', payload)
}

// ---- 分销商管理 ----
export function listDealerUsers(params) {
  return http.get('/store/dealer/users', params)
}

export function approveDealer(id) {
  return http.post(`/store/dealer/users/${id}/approve`)
}

export function rejectDealer(id) {
  return http.post(`/store/dealer/users/${id}/reject`)
}

export function disableDealer(id) {
  return http.post(`/store/dealer/users/${id}/disable`)
}

// ---- 分销订单 ----
export function listDealerOrders(status) {
  return http.get('/store/dealer/orders', status ? { status } : {})
}

// ---- 提现审核 ----
export function listDealerWithdraws(status) {
  return http.get('/store/dealer/withdraws', status ? { status } : {})
}

export function approveDealerWithdraw(id, remark) {
  return http.post(`/store/dealer/withdraws/${id}/approve`, { remark })
}

export function rejectDealerWithdraw(id, remark) {
  return http.post(`/store/dealer/withdraws/${id}/reject`, { remark })
}
