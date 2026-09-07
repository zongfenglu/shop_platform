import http from './http'

// 场次
export function listSeckillTimes() {
  return http.get('/store/seckill/times')
}
export function createSeckillTime(payload) {
  return http.post('/store/seckill/times', payload)
}
export function updateSeckillTime(id, payload) {
  return http.put(`/store/seckill/times/${id}`, payload)
}
export function deleteSeckillTime(id) {
  return http.delete(`/store/seckill/times/${id}`)
}

// 活动
export function listSeckillActives() {
  return http.get('/store/seckill/actives')
}
export function createSeckillActive(payload) {
  return http.post('/store/seckill/actives', payload)
}
export function updateSeckillActive(id, payload) {
  return http.put(`/store/seckill/actives/${id}`, payload)
}
export function deleteSeckillActive(id) {
  return http.delete(`/store/seckill/actives/${id}`)
}

// 活动商品
export function listSeckillGoods(activeId) {
  return http.get(`/store/seckill/actives/${activeId}/goods`)
}
export function createSeckillGoods(payload) {
  return http.post('/store/seckill/goods', payload)
}
export function updateSeckillGoods(id, payload) {
  return http.put(`/store/seckill/goods/${id}`, payload)
}
export function deleteSeckillGoods(id) {
  return http.delete(`/store/seckill/goods/${id}`)
}
