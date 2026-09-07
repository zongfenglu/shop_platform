import http from './http'

export function listStaffUsers() {
  return http.get('/store/staff/users')
}

export function listStaffRoles() {
  return http.get('/store/staff/roles')
}

export function createStaffRole(payload) {
  return http.post('/store/staff/roles', payload)
}

export function updateStaffRole(id, payload) {
  return http.put(`/store/staff/roles/${id}`, payload)
}

export function deleteStaffRole(id) {
  return http.delete(`/store/staff/roles/${id}`)
}

export function createStaff(payload) {
  return http.post('/store/staff/users', payload)
}

export function updateStaff(id, payload) {
  return http.put(`/store/staff/users/${id}`, payload)
}

export function resetStaffPassword(id, password) {
  return http.post(`/store/staff/users/${id}/reset-password`, { password })
}

export function deleteStaff(id) {
  return http.delete(`/store/staff/users/${id}`)
}
