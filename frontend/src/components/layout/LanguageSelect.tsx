import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, ChevronDown } from 'lucide-react'
import { DE, ES, GB, IT, KE, type FlagComponent } from 'country-flag-icons/react/3x2'
import type { Language } from '@/api/types'
import { useAuth } from '@/features/auth/auth-context'
import { currentLanguage, LANGUAGES } from '@/i18n'
import { cn } from '@/lib/format'

/** Kiswahili uses Kenya's flag: Kakuma is in Kenya. SVG flags, because emoji flags show as letters on Windows. */
const FLAGS: Record<Language, FlagComponent> = { en: GB, sw: KE, es: ES, it: IT, de: DE }

function Flag({ code }: { code: Language }) {
  const F = FLAGS[code]
  return <F aria-hidden className="h-4 w-6 shrink-0 rounded-[2px] shadow-[0_0_0_1px_rgba(0,0,0,0.15)]" />
}

export function LanguageSelect({ tone = 'light', id = 'lang-select' }: { tone?: 'light' | 'dark'; id?: string }) {
  const { t } = useTranslation()
  const { setLanguage } = useAuth()
  const current = currentLanguage()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const root = useRef<HTMLDivElement>(null)
  const button = useRef<HTMLButtonElement>(null)
  const list = useRef<HTMLUListElement>(null)
  const labelId = useId()
  const listId = `${id}-list`
  const selected = LANGUAGES.find((l) => l.code === current) ?? LANGUAGES[0]

  useEffect(() => {
    if (!open) return
    list.current?.focus()
    const onPointer = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointer)
    return () => document.removeEventListener('pointerdown', onPointer)
  }, [open])

  function openList() {
    setActive(Math.max(0, LANGUAGES.findIndex((l) => l.code === current)))
    setOpen(true)
  }

  function choose(code: Language) {
    setOpen(false)
    button.current?.focus()
    if (code !== current) setLanguage(code)
  }

  function onButtonKey(e: KeyboardEvent) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      openList()
    }
  }

  function onListKey(e: KeyboardEvent) {
    const last = LANGUAGES.length - 1
    const moves: Record<string, () => number> = {
      ArrowDown: () => Math.min(last, active + 1),
      ArrowUp: () => Math.max(0, active - 1),
      Home: () => 0,
      End: () => last,
    }
    if (moves[e.key]) {
      e.preventDefault()
      setActive(moves[e.key]())
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      choose(LANGUAGES[active].code)
    } else if (e.key === 'Escape' || e.key === 'Tab') {
      if (e.key === 'Escape') e.preventDefault()
      setOpen(false)
      if (e.key === 'Escape') button.current?.focus()
    }
  }

  const dark = tone === 'dark'
  return (
    <div ref={root} className="relative">
      <span id={labelId} className="sr-only">
        {t('common.language')}
      </span>
      <button
        ref={button}
        id={id}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-labelledby={`${labelId} ${id}`}
        onClick={() => (open ? setOpen(false) : openList())}
        onKeyDown={onButtonKey}
        className={cn(
          'flex min-h-10 w-full min-w-40 cursor-pointer items-center gap-2.5 rounded-md border ps-3 pe-3 text-sm font-medium transition-colors',
          dark ? 'border-white/25 bg-white/10 text-white hover:bg-white/15' : 'border-border bg-surface text-text hover:border-muted',
        )}
      >
        <Flag code={selected.code} />
        <span lang={selected.code} className="flex-1 text-start">
          {selected.label}
        </span>
        <ChevronDown size={16} aria-hidden className={cn('ms-2 shrink-0 transition-transform', open && 'rotate-180')} />
      </button>
      {open && (
        <ul
          ref={list}
          id={listId}
          role="listbox"
          tabIndex={-1}
          aria-labelledby={labelId}
          aria-activedescendant={`${id}-opt-${LANGUAGES[active].code}`}
          onKeyDown={onListKey}
          className="absolute end-0 z-50 mt-2 w-full min-w-56 rounded-lg border border-border bg-surface p-1.5 text-text shadow-lg focus:outline-none"
        >
          {LANGUAGES.map((l, i) => (
            <li
              key={l.code}
              id={`${id}-opt-${l.code}`}
              role="option"
              lang={l.code}
              aria-selected={l.code === current}
              onPointerEnter={() => setActive(i)}
              onClick={() => choose(l.code)}
              className={cn('flex min-h-11 cursor-pointer items-center gap-3 rounded-md px-3 text-[15px]', i === active && 'bg-hover', l.code === current && 'font-semibold text-primary')}
            >
              <Flag code={l.code} />
              <span className="flex-1">{l.label}</span>
              {l.code === current && <Check size={16} aria-hidden />}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
