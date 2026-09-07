import http from './http'

/** 套餐模板管理。对应 AdminPackageController（/admin/packages）。 */
export function listPackages() {
  return http.get('/admin/packages')
}

export function getPackage(id) {
  return http.get(`/admin/packages/${id}`)
}

export function createPackage(payload) {
  return http.post('/admin/packages', payload)
}

export function updatePackage(id, payload) {
  return http.put(`/admin/packages/${id}`, payload)
}

export function togglePackageShow(id, show) {
  return http.put(`/admin/packages/${id}/show`, null, { params: { show } })
}
