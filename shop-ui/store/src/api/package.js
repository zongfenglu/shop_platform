import http from './http'

export function getStorePackage() {
  return http.get('/store/package')
}

export function placePackageOrder(payload) {
  return http.post('/store/package/orders', payload)
}

export function listStoreInvoices() {
  return http.get('/store/package/invoices')
}

export function applyStoreInvoice(payload) {
  return http.post('/store/package/invoices', payload)
}
