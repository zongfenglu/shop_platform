import http from './http'

const base = '/store/settings'

export const listExpressCompanies = (enabledOnly = false) =>
  http.get(`${base}/express-companies${enabledOnly ? '/enabled' : ''}`)
export const createExpressCompany = (data) => http.post(`${base}/express-companies`, data)
export const updateExpressCompany = (id, data) => http.put(`${base}/express-companies/${id}`, data)
export const deleteExpressCompany = (id) => http.delete(`${base}/express-companies/${id}`)

export const listReturnAddresses = () => http.get(`${base}/return-addresses`)
export const createReturnAddress = (data) => http.post(`${base}/return-addresses`, data)
export const updateReturnAddress = (id, data) => http.put(`${base}/return-addresses/${id}`, data)
export const deleteReturnAddress = (id) => http.delete(`${base}/return-addresses/${id}`)
export const setDefaultReturnAddress = (id) => http.put(`${base}/return-addresses/${id}/default`)

export const listPrinters = () => http.get(`${base}/printers`)
export const createPrinter = (data) => http.post(`${base}/printers`, data)
export const updatePrinter = (id, data) => http.put(`${base}/printers/${id}`, data)
export const deletePrinter = (id) => http.delete(`${base}/printers/${id}`)

export const listSmsChannels = () => http.get(`${base}/sms-channels`)
export const createSmsChannel = (data) => http.post(`${base}/sms-channels`, data)
export const updateSmsChannel = (id, data) => http.put(`${base}/sms-channels/${id}`, data)
export const deleteSmsChannel = (id) => http.delete(`${base}/sms-channels/${id}`)

export const getOperationSettings = () => http.get(`${base}/operations`)
export const saveUploadSettings = (data) => http.put(`${base}/upload`, data)
export const savePrintRules = (data) => http.put(`${base}/print-rules`, data)
export const saveSmsRules = (data) => http.put(`${base}/sms-rules`, data)
