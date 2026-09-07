import http from './http'

export function pagePlatformUsers(params) {
  return http.get('/admin/platform-users', { params })
}

export function createPlatformUser(payload) {
  return http.post('/admin/platform-users', payload)
}

export function updatePlatformUser(id, payload) {
  return http.put(`/admin/platform-users/${id}`, payload)
}

export function resetPlatformUserPassword(id, password) {
  return http.post(`/admin/platform-users/${id}/reset-password`, { password })
}

export function togglePlatformUserStatus(id) {
  return http.post(`/admin/platform-users/${id}/toggle-status`)
}

export function listPlatformRoles() {
  return http.get('/admin/platform-users/roles')
}

export function listPlatformMenus() {
  return http.get('/admin/platform-users/menus')
}

export function getRoleMenus(roleId) {
  return http.get(`/admin/platform-users/roles/${roleId}/menus`)
}

export function saveRoleMenus(roleId, menuIds) {
  return http.put(`/admin/platform-users/roles/${roleId}/menus`, { menuIds })
}

export function pageSysLogs(params) {
  return http.get('/admin/logs', { params })
}
