import http from './http'

/**
 * 微信支付配置。对应 StorePayConfigController（/store/pay-config）。
 * 密钥只进不出：GET 只回显掩码信息（apiV3KeySet/mchPrivateKeySet 布尔值），
 * 保存后不能再次显示明文，前端不应该也不能力图"回填"这些密钥字段。
 */
export function getPayConfig() {
  return http.get('/store/pay-config')
}

export function savePayConfig(payload) {
  return http.post('/store/pay-config', payload)
}
