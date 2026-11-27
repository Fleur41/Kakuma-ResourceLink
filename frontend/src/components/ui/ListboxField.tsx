import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { Check, ChevronDown } from 'lucide-react'
import { cn } from '@/lib/format'

export interface ListboxOption<T extends string> {
  value: T
  label: string
  /** Shown before the label, in the button and in the list (e.g. a flag). */
  icon?: ReactNode
  lang?: string
}

interface ListboxFieldProps<T extends string> {
  label: string
  value: T
  onChange: (value: T) => void
  options: ListboxOption<T>[]
  id?: string
}

/** A styled replacement for a native <select>: the arrow sits inside the field and options can show an icon. */
export function ListboxField<T extends string>({ label, value, onChange, options, id: idProp }: ListboxFieldProps<T>) {
  const auto = useId()
  const id = idProp ?? auto
  const labelId = `${id}-label`
  const listId = `${id}-list`
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const root = useRef<HTMLDivElement>(null)
  const button = useRef<HTMLButtonElement>(null)
  const list = useRef<HTMLUListElement>(null)
  const selected = options.find((o) => o.value === value) ?? options[0]

  useEffect(() => {
    if (!open) return
    list.current?.focus()
    const onPointer = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointer)
    return () => document.removeEventListener('pointerdown', onPointer)
  }, [open])

  useEffect(() => {
    if (open) list.current?.querySelector(`[data-index="${active}"]`)?.scrollIntoView({ block: 'nearest' })
  }, [open, active])

  function openList() {
    setActive(Math.max(0, options.findIndex((o) => o.value === value)))
    setOpen(true)
  }

  function choose(v: T) {
    setOpen(false)
    button.current?.focus()
    if (v !== value) onChange(v)
  }

  function onButtonKey(e: KeyboardEvent) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      openList()
    }
  }

  function onListKey(e: KeyboardEvent) {
    const last = options.length - 1
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
      choose(options[active].value)
    } else if (e.key === 'Escape' || e.key === 'Tab') {
      if (e.key === 'Escape') e.preventDefault()
      setOpen(false)
      if (e.key === 'Escape') button.current?.focus()
    }
  }

  return (
    <div className="space-y-1.5">
      <span id={labelId} className="block text-sm font-medium text-text">
        {label}
      </span>
      <div ref={root} className="relative">
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
            'flex w-full cursor-pointer items-center gap-3 border bg-surface px-4 py-3 text-start text-[15px] text-text transition-colors',
            'hover:border-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary',
            open ? 'border-primary ring-1 ring-primary' : 'border-border',
          )}
        >
          {selected?.icon}
          <span lang={selected?.lang} className="flex-1 truncate">
            {selected?.label}
          </span>
          <ChevronDown size={18} aria-hidden className={cn('shrink-0 text-muted transition-transform', open && 'rotate-180')} />
        </button>
        {open && (
          <ul
            ref={list}
            id={listId}
            role="listbox"
            tabIndex={-1}
            aria-labelledby={labelId}
            aria-activedescendant={`${id}-opt-${options[active].value}`}
            onKeyDown={onListKey}
            className="absolute inset-x-0 z-50 mt-1.5 max-h-72 overflow-y-auto rounded-xl border border-border bg-surface p-1.5 text-text shadow-lg focus:outline-none"
          >
            {options.map((o, i) => (
              <li
                key={o.value}
                id={`${id}-opt-${o.value}`}
                data-index={i}
                role="option"
                lang={o.lang}
                aria-selected={o.value === value}
                onPointerEnter={() => setActive(i)}
                onClick={() => choose(o.value)}
                className={cn(
                  'flex min-h-11 cursor-pointer items-center gap-3 rounded-md px-3 text-[15px]',
                  i === active && 'bg-hover',
                  o.value === value && 'font-semibold text-primary',
                )}
              >
                {o.icon}
                <span className="flex-1">{o.label}</span>
                {o.value === value && <Check size={16} aria-hidden />}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
