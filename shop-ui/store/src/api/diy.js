import http from './http'

export function listDiyPages() {
  return http.get('/store/diy/pages')
}

export function createDiyPage(payload) {
  return http.post('/store/diy/pages', payload)
}

export function copyDiyPage(id, name) {
  return http.post(`/store/diy/pages/${id}/copy`, { name })
}

export function setDiyPageHome(id) {
  return http.put(`/store/diy/pages/${id}/home`)
}

export function deleteDiyPage(id) {
  return http.delete(`/store/diy/pages/${id}`)
}

export function updateDiyPageDraft(id, draftData) {
  return http.put(`/store/diy/pages/${id}/draft`, { draftData })
}

export function publishDiyPage(id) {
  return http.post(`/store/diy/pages/${id}/publish`)
}

export function applyDiyTemplate(templateId, name) {
  return http.post('/store/diy/pages/from-template', { templateId, name })
}

export function listDiyTemplates() {
  return http.get('/store/diy/templates')
}

export function getDiyTabbar() {
  return http.get('/store/diy/tabbar')
}

export function saveDiyTabbar(items, style) {
  return http.put('/store/diy/tabbar', { items, style })
}

export function getDiyMenus() {
  return http.get('/store/diy/menus')
}

export function listMyTemplates() {
  return http.get('/store/diy/my-templates')
}

export function saveMyTemplate(name, pageData) {
  return http.post('/store/diy/my-templates', { name, pageData })
}

/** 单个页面（含 draftData），编辑器全屏路由靠它按 id 直接加载，支持刷新/深链。 */
export function getDiyPage(id) {
  return http.get(`/store/diy/pages/${id}`)
}

export function getCategoryPage() {
  return http.get('/store/diy/category-page')
}

export function saveCategoryPage(payload) {
  return http.put('/store/diy/category-page', payload)
}
