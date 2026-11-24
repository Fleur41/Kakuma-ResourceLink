import type { ReactNode } from 'react'
import { useDocumentTitle } from '@/lib/hooks'

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  useDocumentTitle(title)
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-[28px] leading-tight font-bold md:text-[32px]">{title}</h1>
        {subtitle && <p className="mt-2 text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
    </header>
  )
}

export function SearchInput({ value, onChange, placeholder, label }: { value: string; onChange: (v: string) => void; placeholder: string; label: string }) {
  return (
    <div className="relative">
      <label className="sr-only" htmlFor="page-search">
        {label}
      </label>
      <input
        id="page-search"
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-border bg-surface px-5 py-3.5 text-[15px] shadow-card placeholder:text-muted/80 focus:border-primary focus:outline-none focus:ring-2 focus:ring-accent/30"
      />
    </div>
  )
}
