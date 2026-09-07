import http from './http'

/** 全部兑换项 */
export function listPointsGoods() {
  return http.get('/store/points-goods')
}

/** 新建兑换项 */
export function createPointsGoods(payload) {
  return http.post('/store/points-goods', payload)
}

/** 编辑兑换项 */
export function updatePointsGoods(id, payload) {
  return http.put(`/store/points-goods/${id}`, payload)
}

/** 删除兑换项 */
export function deletePointsGoods(id) {
  return http.delete(`/store/points-goods/${id}`)
}

/** 兑换记录 */
export function listExchangeRecords(status) {
  return http.get('/store/points-goods/exchanges', status ? { status } : {})
}
