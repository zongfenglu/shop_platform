import http from './http'

export function pageDomains(params) {
  return http.get('/admin/domains', { params })
}

export function checkDomainCname(id) {
  return http.post(`/admin/domains/${id}/cname-check`)
}

export function approveDomain(id) {
  return http.post(`/admin/domains/${id}/approve`)
}

export function rejectDomain(id, reason) {
  return http.post(`/admin/domains/${id}/reject`, { reason })
}

export function unbindDomain(id) {
  return http.post(`/admin/domains/${id}/unbind`)
}

export function issueDomainCert(id) {
  return http.post(`/admin/domains/${id}/issue-cert`)
}
