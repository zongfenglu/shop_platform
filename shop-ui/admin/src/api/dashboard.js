import http from './http'

export function getAdminDashboard() {
  return http.get('/admin/dashboard/overview')
}
