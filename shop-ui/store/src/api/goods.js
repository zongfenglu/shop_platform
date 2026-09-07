import http from './http'

/**
 * 商品管理。对应 StoreGoodsController（/store/goods）。
 * 租户隔离由后端 TenantContext 自动完成，前端不传也不能传 shopId。
 */
export function pageGoods(params) {
  return http.get('/store/goods', { params })
}

export function getGoods(id) {
  return http.get(`/store/goods/${id}`)
}

/** 发布商品。对应 StoreGoodsController#publish。 */
export function publishGoods(payload) {
  return http.post('/store/goods', payload)
}

export function updateGoods(id, payload) {
  return http.put(`/store/goods/${id}`, payload)
}

export function updateGoodsStatus(id, status) {
  return http.put(`/store/goods/${id}/status`, null, { params: { status } })
}

/** 商品分类（三级树）。对应 StoreGoodsCategoryController。 */
export function listCategories() {
  return http.get('/store/goods-categories')
}

export function createCategory(payload) {
  return http.post('/store/goods-categories', payload)
}

export function updateCategory(id, payload) {
  return http.put(`/store/goods-categories/${id}`, payload)
}

export function deleteCategory(id) {
  return http.delete(`/store/goods-categories/${id}`)
}

/** 运费模板。对应 StoreFreightTemplateController。 */
export function listFreightTemplates() {
  return http.get('/store/freight-templates')
}

export function createFreightTemplate(payload) {
  return http.post('/store/freight-templates', payload)
}

export function updateFreightTemplate(id, payload) {
  return http.put(`/store/freight-templates/${id}`, payload)
}

export function deleteFreightTemplate(id) {
  return http.delete(`/store/freight-templates/${id}`)
}
