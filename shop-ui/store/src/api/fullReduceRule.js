import http from './http'

export function listFullReduceRules() {
  return http.get('/store/full-reduce-rules')
}

export function createFullReduceRule(payload) {
  return http.post('/store/full-reduce-rules', payload)
}

export function updateFullReduceRule(id, payload) {
  return http.put(`/store/full-reduce-rules/${id}`, payload)
}

export function deleteFullReduceRule(id) {
  return http.delete(`/store/full-reduce-rules/${id}`)
}
