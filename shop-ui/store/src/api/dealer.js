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
  return http.get('/store/dealer/users', { params })
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
  // axios.get 第二参是 config，查询参数必须放 params —— 直接传 { status } 会被当 config 丢掉
  return http.get('/store/dealer/orders', { params: status ? { status } : {} })
}

// ---- 提现审核 ----
export function listDealerWithdraws(status) {
  return http.get('/store/dealer/withdraws', { params: status ? { status } : {} })
}

export function approveDealerWithdraw(id, remark) {
  return http.post(`/store/dealer/withdraws/${id}/approve`, { remark })
}

export function rejectDealerWithdraw(id, remark) {
  return http.post(`/store/dealer/withdraws/${id}/reject`, { remark })
}
