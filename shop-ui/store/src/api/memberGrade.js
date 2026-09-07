import http from './http'

export function listMemberGrades() {
  return http.get('/store/member-grades')
}

export function createMemberGrade(payload) {
  return http.post('/store/member-grades', payload)
}

export function updateMemberGrade(id, payload) {
  return http.put(`/store/member-grades/${id}`, payload)
}

export function deleteMemberGrade(id) {
  return http.delete(`/store/member-grades/${id}`)
}
