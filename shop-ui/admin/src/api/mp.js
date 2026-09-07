import http from './http'

export function getAdminMpOverview() {
  return http.get('/admin/mp/overview')
}

export function listAdminMpAuthorizers() {
  return http.get('/admin/mp/authorizers')
}

export function listAdminMpTemplates() {
  return http.get('/admin/mp/templates')
}

export function saveAdminMpTemplate(payload) {
  return http.post('/admin/mp/templates', payload)
}

export function disableAdminMpTemplate(id) {
  return http.put(`/admin/mp/templates/${id}/disable`)
}

export function getAdminMpExtJson(shopId, appType) {
  return http.get('/admin/mp/ext-json', { params: { shopId, appType } })
}
