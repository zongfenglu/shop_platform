import http from './http'

/**
 * 商品规格管理（规格库，可跨商品复用）。对应 StoreGoodsSpecController（/store/goods-specs）。
 * list() 一次性返回规格及其规格值（嵌套结构），发布商品页据此渲染"颜色/尺码"标签组。
 */
export function listSpecs() {
  return http.get('/store/goods-specs')
}

export function createSpec(name) {
  return http.post('/store/goods-specs', { name })
}

export function createSpecValue(specId, value) {
  return http.post(`/store/goods-specs/${specId}/values`, { value })
}

export function deleteSpecValue(specId, valueId) {
  return http.delete(`/store/goods-specs/${specId}/values/${valueId}`)
}
