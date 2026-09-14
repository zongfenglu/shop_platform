/**
 * Format a backend LocalDateTime value without applying a timezone conversion.
 * Example: 2026-09-14T09:41:09 -> 2026-09-14 09:41
 */
export function formatDateTime(value) {
  if (!value) return ''

  const match = String(value).match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})/)
  return match ? `${match[1]} ${match[2]}` : String(value).replace('T', ' ')
}
