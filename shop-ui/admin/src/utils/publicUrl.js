/**
 * 管理后台打开消费者 H5 时使用的公网地址。
 * 生产构建优先读取显式配置；未配置时按 admin.* -> h5.* 推导，方便域名化部署。
 */
export function getH5PublicUrl() {
  const configured = String(import.meta.env.VITE_H5_PUBLIC_URL || '').trim()
  if (configured) return configured.replace(/\/+$/, '')

  const { protocol, hostname } = window.location
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:5175'
  }

  if (hostname.startsWith('admin.')) {
    return `${protocol}//h5.${hostname.slice('admin.'.length)}`
  }

  return `${protocol}//${hostname}:8092`
}

