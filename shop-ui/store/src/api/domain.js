import http from './http'

export function getStoreDomains() {
  return http.get('/store/domains')
}

export function applyStoreDomain(domain) {
  return http.post('/store/domains', { domain })
}

export function checkStoreDomainCname(id) {
  return http.post(`/store/domains/${id}/cname-check`)
}

export function unbindStoreDomain(id) {
  return http.delete(`/store/domains/${id}`)
}
