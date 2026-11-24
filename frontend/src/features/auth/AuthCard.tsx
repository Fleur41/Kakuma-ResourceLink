import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '@/lib/hooks'

export function AuthCard({ title, subtitle, children, footer }: { title: string; subtitle?: string; children: ReactNode; footer?: ReactNode }) {
  const { t } = useTranslation()
  useDocumentTitle(title)
  return (
    <div className="mx-auto grid max-w-[980px] overflow-hidden rounded-xl border border-border bg-surface shadow-card md:grid-cols-[1fr_1.1fr]">
      <div className="hero-gradient hidden flex-col justify-end gap-3 p-10 text-white md:flex">
        <p className="text-xs font-bold tracking-[0.12em] uppercase opacity-90">Kakuma ResourceLink</p>
        <p className="font-display text-2xl leading-snug font-bold">{t('brand.tagline')}</p>
      </div>
      <div className="p-6 sm:p-10">
        <h1 className="text-2xl font-bold">{title}</h1>
        {subtitle && <p className="mt-2 text-muted">{subtitle}</p>}
        <div className="mt-6">{children}</div>
        {footer && <div className="mt-6 text-center text-sm text-muted">{footer}</div>}
      </div>
    </div>
  )
}
