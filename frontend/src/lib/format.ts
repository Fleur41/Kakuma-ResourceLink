import { currentLanguage } from '@/i18n'

const LOCALES = { en: 'en-GB', sw: 'sw-KE', es: 'es-ES', it: 'it-IT', de: 'de-DE' } as const

export function locale(): string {
  return LOCALES[currentLanguage()]
}

export function formatDate(value?: string | null, opts: Intl.DateTimeFormatOptions = { dateStyle: 'medium' }): string {
  if (!value) return ''
  const d = new Date(value.length === 10 ? `${value}T00:00:00` : value)
  if (Number.isNaN(d.getTime())) return ''
  return new Intl.DateTimeFormat(locale(), opts).format(d)
}

/** "3 days ago" / "in 5 days", localized by the browser's Intl data. */
export function formatRelative(value?: string | null, now = Date.now()): string {
  if (!value) return ''
  const t = new Date(value.length === 10 ? `${value}T00:00:00` : value).getTime()
  if (Number.isNaN(t)) return ''
  const diff = (t - now) / 1000
  const rtf = new Intl.RelativeTimeFormat(locale(), { numeric: 'auto' })
  const abs = Math.abs(diff)
  if (abs < 60) return rtf.format(Math.round(diff), 'second')
  if (abs < 3600) return rtf.format(Math.round(diff / 60), 'minute')
  if (abs < 86400) return rtf.format(Math.round(diff / 3600), 'hour')
  if (abs < 86400 * 30) return rtf.format(Math.round(diff / 86400), 'day')
  return formatDate(value)
}

/** Whole days from today until a YYYY-MM-DD deadline (negative once passed). */
export function daysUntil(date: string, today = new Date()): number {
  const d = new Date(`${date}T00:00:00`)
  const t = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return Math.round((d.getTime() - t.getTime()) / 86400000)
}

export function formatAmount(amount: string | number, currency: string): string {
  const n = typeof amount === 'string' ? Number(amount) : amount
  if (!Number.isFinite(n)) return `${amount} ${currency}`
  try {
    return new Intl.NumberFormat(locale(), { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
  } catch {
    return `${n.toLocaleString(locale())} ${currency}`
  }
}

export function initials(name?: string | null): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? (parts[parts.length - 1][0] ?? '') : '')).toUpperCase() || '?'
}

export function cn(...classes: (string | false | null | undefined)[]): string {
  return classes.filter(Boolean).join(' ')
}

/** Comma-separated tags → trimmed, de-duplicated list (max 20). */
export function parseTags(input: string): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const raw of input.split(',')) {
    const tag = raw.trim()
    if (!tag || seen.has(tag.toLowerCase())) continue
    seen.add(tag.toLowerCase())
    out.push(tag.slice(0, 50))
    if (out.length === 20) break
  }
  return out
}

export function wordCount(text: string): number {
  return text.trim() ? text.trim().split(/\s+/).length : 0
}
