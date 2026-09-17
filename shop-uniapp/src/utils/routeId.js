const SNOWFLAKE_ROUTE_PREFIX = 'sf_'

/**
 * 小程序路由会在部分链路把纯数字查询参数转成 Number，19 位雪花 ID 会因此丢失精度。
 * 加非数字前缀后再传递，目标页解码即可始终保留原始十进制字符串。
 */
export function encodeRouteId(value) {
  if (value == null || value === '') return ''
  const text = String(value)
  return text.startsWith(SNOWFLAKE_ROUTE_PREFIX) ? text : SNOWFLAKE_ROUTE_PREFIX + text
}

export function decodeRouteId(value) {
  if (value == null || value === '') return ''
  const text = String(value)
  return text.startsWith(SNOWFLAKE_ROUTE_PREFIX) ? text.slice(SNOWFLAKE_ROUTE_PREFIX.length) : text
}
