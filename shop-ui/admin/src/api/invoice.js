import http from './http'

export function listInvoices(params) {
  return http.get('/admin/invoices', { params })
}

export function issueInvoice(id, payload) {
  return http.post(`/admin/invoices/${id}/issue`, payload || {})
}

export function rejectInvoice(id, payload) {
  return http.post(`/admin/invoices/${id}/reject`, payload || {})
}
