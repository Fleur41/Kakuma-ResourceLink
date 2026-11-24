import type { ReactNode } from 'react'
import { Search } from 'lucide-react'
import { useDocumentTitle } from '@/lib/hooks'

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  useDocumentTitle(title)
  return (
    <header className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-border pb-6">
      <div className="min-w-0">
        <h1 className="text-[32px] leading-tight font-light md:text-[42px]">{title}</h1>
        {subtitle && <p className="mt-2 text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
    </header>
  )
}

export function SearchInput({ value, onChange, placeholder, label }: { value: string; onChange: (v: string) => void; placeholder: string; label: string }) {
  return (
    <div className="relative">
      <Search aria-hidden size={20} className="pointer-events-none absolute start-5 top-1/2 -translate-y-1/2 text-muted" />
      <label className="sr-only" htmlFor="page-search">
        {label}
      </label>
      <input
        id="page-search"
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-none border-0 border-b border-muted/60 bg-surface py-4 ps-13 pe-5 text-[15px] transition-colors placeholder:text-muted/80 hover:bg-hover focus:border-primary focus:outline-none focus:ring-0 focus:shadow-[inset_0_-2px_0_var(--kk-primary)]"
      />
    </div>
  )
}
