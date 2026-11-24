import { useTranslation } from 'react-i18next'
import { Globe } from 'lucide-react'
import type { Language } from '@/api/types'
import { useAuth } from '@/features/auth/auth-context'
import { currentLanguage, LANGUAGES } from '@/i18n'
import { cn } from '@/lib/format'

export function LanguageSelect({ tone = 'light', id = 'lang-select' }: { tone?: 'light' | 'dark'; id?: string }) {
  const { t } = useTranslation()
  const { setLanguage } = useAuth()
  return (
    <div className={cn('relative flex items-center', tone === 'dark' ? 'text-white' : 'text-text')}>
      <Globe size={16} aria-hidden className="pointer-events-none absolute start-3" />
      <label htmlFor={id} className="sr-only">
        {t('common.language')}
      </label>
      <select
        id={id}
        value={currentLanguage()}
        onChange={(e) => setLanguage(e.target.value as Language)}
        className={cn(
          'min-h-10 cursor-pointer rounded-md border ps-9 pe-3 text-sm font-medium',
          tone === 'dark' ? 'border-white/25 bg-white/10 text-white [&>option]:text-[#202B29]' : 'border-border bg-surface',
        )}
      >
        {LANGUAGES.map((l) => (
          <option key={l.code} value={l.code} lang={l.code}>
            {l.label}
          </option>
        ))}
      </select>
    </div>
  )
}
