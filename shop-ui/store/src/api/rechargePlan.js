import http from './http'

export function listRechargePlans() {
  return http.get('/store/recharge-plans')
}

export function createRechargePlan(payload) {
  return http.post('/store/recharge-plans', payload)
}

export function updateRechargePlan(id, payload) {
  return http.put(`/store/recharge-plans/${id}`, payload)
}

export function deleteRechargePlan(id) {
  return http.delete(`/store/recharge-plans/${id}`)
}
