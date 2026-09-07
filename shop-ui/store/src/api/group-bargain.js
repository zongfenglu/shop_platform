import http from './http'

// 拼团活动
export function listGroupActives() {
  return http.get('/store/group/actives')
}
export function createGroupActive(payload) {
  return http.post('/store/group/actives', payload)
}
export function updateGroupActive(id, payload) {
  return http.put(`/store/group/actives/${id}`, payload)
}
export function deleteGroupActive(id) {
  return http.delete(`/store/group/actives/${id}`)
}

// 砍价活动
export function listBargainActives() {
  return http.get('/store/bargain/actives')
}
export function createBargainActive(payload) {
  return http.post('/store/bargain/actives', payload)
}
export function updateBargainActive(id, payload) {
  return http.put(`/store/bargain/actives/${id}`, payload)
}
export function deleteBargainActive(id) {
  return http.delete(`/store/bargain/actives/${id}`)
}
