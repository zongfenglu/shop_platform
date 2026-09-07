import http from './http'

export function getOpsOverview() {
  return http.get('/admin/ops/overview')
}

export function listOpsJobs() {
  return http.get('/admin/ops/jobs')
}

export function runOpsJob(code) {
  return http.post(`/admin/ops/jobs/${code}/run`)
}

export function listDailyStats(params) {
  return http.get('/admin/ops/daily', { params })
}

export function listBackups() {
  return http.get('/admin/ops/backups')
}

export function createBackup() {
  return http.post('/admin/ops/backups', null, { timeout: 120000 })
}

export function getOpsQueues() {
  return http.get('/admin/ops/queues')
}

export function getOpsCache() {
  return http.get('/admin/ops/cache')
}

export function flushOpsCache(payload) {
  return http.post('/admin/ops/cache/flush', payload)
}

export async function downloadBackup(id, filename) {
  const blob = await http.get(`/admin/ops/backups/${id}/file`, { responseType: 'blob', timeout: 120000 })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || `backup-${id}.sql`
  a.click()
  URL.revokeObjectURL(url)
}
