import http from './http'

export function listArticleCategories() {
  return http.get('/store/content/article-categories')
}

export function createArticleCategory(payload) {
  return http.post('/store/content/article-categories', payload)
}

export function updateArticleCategory(id, payload) {
  return http.put(`/store/content/article-categories/${id}`, payload)
}

export function deleteArticleCategory(id) {
  return http.delete(`/store/content/article-categories/${id}`)
}

export function pageArticles(params) {
  return http.get('/store/content/articles', { params })
}

export function getArticle(id) {
  return http.get(`/store/content/articles/${id}`)
}

export function createArticle(payload) {
  return http.post('/store/content/articles', payload)
}

export function updateArticle(id, payload) {
  return http.put(`/store/content/articles/${id}`, payload)
}

export function deleteArticle(id) {
  return http.delete(`/store/content/articles/${id}`)
}
