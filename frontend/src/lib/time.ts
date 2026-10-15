const rtf = new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' })

/** 相对时间：刚刚 / x 分钟前 / x 小时前 / x 天前，超过一个月显示日期。 */
export function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diffMs / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return rtf.format(-minutes, 'minute')
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return rtf.format(-hours, 'hour')
  const days = Math.floor(hours / 24)
  if (days < 30) return rtf.format(-days, 'day')
  return new Date(iso).toLocaleDateString('zh-CN')
}
