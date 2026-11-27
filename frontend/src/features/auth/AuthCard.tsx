import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useDocumentTitle } from '@/lib/hooks'

export function AuthCard({ title, subtitle, children, footer }: { title: string; subtitle?: string; children: ReactNode; footer?: ReactNode }) {
  const { t } = useTranslation()
  useDocumentTitle(title)
  return (
    <div className="mx-auto grid max-w-[980px] overflow-hidden rounded-xl border border-border bg-surface shadow-card md:grid-cols-[1fr_1.1fr]">
      <div className="relative hidden flex-col overflow-hidden bg-[#0b1714] p-10 text-white md:flex">
        {/* Soft brand glow and faint rings behind the mark */}
        <div aria-hidden className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_38%,rgba(18,166,129,0.38),transparent_58%),linear-gradient(180deg,#0d2a23_0%,#0b1714_70%)]" />
        <svg aria-hidden viewBox="0 0 400 400" className="pointer-events-none absolute top-[38%] left-1/2 w-[135%] -translate-x-1/2 -translate-y-1/2 text-white/[0.06]">
          <circle cx="200" cy="200" r="120" fill="none" stroke="currentColor" />
          <circle cx="200" cy="200" r="160" fill="none" stroke="currentColor" />
          <circle cx="200" cy="200" r="198" fill="none" stroke="currentColor" />
        </svg>
        <Link to="/" aria-label="Kakuma ResourceLink" className="relative flex flex-1 items-center justify-center py-6">
          <BrandMark className="w-[min(62%,260px)] drop-shadow-[0_18px_40px_rgba(0,0,0,0.45)]" />
        </Link>
        <div className="relative space-y-3">
          <p className="text-xs font-bold tracking-[0.12em] text-[#7fe0c4] uppercase">Kakuma ResourceLink</p>
          <p className="font-display text-2xl leading-snug font-bold">{t('brand.tagline')}</p>
        </div>
      </div>
      <div className="p-6 sm:p-10">
        <Link to="/" aria-label="Kakuma ResourceLink" className="mb-6 flex w-fit items-center gap-2.5 md:hidden">
          <img src="/logo.svg" alt="" width={44} height={44} />
          <span className="text-sm font-bold tracking-[0.08em] uppercase">Kakuma ResourceLink</span>
        </Link>
        <h1 className="text-2xl font-bold">{title}</h1>
        {subtitle && <p className="mt-2 text-muted">{subtitle}</p>}
        <div className="mt-6">{children}</div>
        {footer && <div className="mt-6 text-center text-sm text-muted">{footer}</div>}
      </div>
    </div>
  )
}

/** The app's dove-in-a-ring mark (same shapes as the Android launch screen), drawn large with gradients. */
function BrandMark({ className }: { className?: string }) {
  const id = useId()
  return (
    <svg viewBox="0 0 48 48" aria-hidden className={className}>
      <defs>
        <linearGradient id={`${id}-ring`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#7fe0c4" />
          <stop offset="1" stopColor="#12A681" />
        </linearGradient>
        <linearGradient id={`${id}-wing`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#d6f6ec" />
          <stop offset="1" stopColor="#8fdcc4" />
        </linearGradient>
      </defs>
      <path d="M24 4A20 20 0 1 1 7 34" fill="none" stroke={`url(#${id}-ring)`} strokeWidth="2.4" strokeLinecap="round" />
      <path fill="#fff" d="M9 29C15 21 23 19 29 21 33 15 39 12 44 12 40 16 38 20 37 24 36 32 28 38 18 36 22 34 24 32 25 30 20 32 14 31 9 29Z" />
      <path fill={`url(#${id}-wing)`} d="M21 23C19 15 22 8 28 4 28 12 30 16 30 21Z" />
      <circle cx="39" cy="15.5" r="1.2" fill="#F5A623" />
    </svg>
  )
}
