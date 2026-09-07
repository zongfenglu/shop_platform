import http from './http'

export function pageMembers(params) {
  return http.get('/store/members', { params })
}

export function getMember(id) {
  return http.get(`/store/members/${id}`)
}

export function getMemberBalanceLogs(id, params) {
  return http.get(`/store/members/${id}/balance-logs`, { params })
}

export function getMemberPointsLogs(id, params) {
  return http.get(`/store/members/${id}/points-logs`, { params })
}

export function adjustMemberBalance(id, payload) {
  return http.post(`/store/members/${id}/adjust-balance`, payload)
}

export function adjustMemberPoints(id, payload) {
  return http.post(`/store/members/${id}/adjust-points`, payload)
}
