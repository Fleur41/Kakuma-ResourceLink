import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { cn } from '@/lib/format'
import { ToastContext, type ToastAction, type ToastApi, type ToastKind } from './toast-context'

interface ToastItem {
  id: number
  message: string
  kind: ToastKind
  action?: ToastAction
}

const icons = { success: CheckCircle2, error: AlertCircle, info: Info }

/**
 * Short toasts for quick confirmations ("Saved"). A toast with an action (Retry/Undo) stays longer.
 * Rendered in a polite live region so screen readers announce them without stealing focus.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const [items, setItems] = useState<ToastItem[]>([])
  const nextId = useRef(1)

  const dismiss = useCallback((id: number) => setItems((list) => list.filter((i) => i.id !== id)), [])

  const show = useCallback<ToastApi['show']>(
    (message, opts = {}) => {
      const id = nextId.current++
      const item: ToastItem = { id, message, kind: opts.kind ?? 'info', action: opts.action }
      // Keep at most 3 on screen; drop an identical message instead of stacking duplicates.
      setItems((list) => [...list.filter((i) => i.message !== message).slice(-2), item])
      window.setTimeout(() => dismiss(id), opts.duration ?? (opts.action ? 6000 : 2800))
    },
    [dismiss],
  )

  const api = useMemo<ToastApi>(
    () => ({
      show,
      success: (m) => show(m, { kind: 'success' }),
      error: (m, action) => show(m, { kind: 'error', action }),
      info: (m) => show(m, { kind: 'info' }),
    }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div aria-live="polite" role="status" className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
        {items.map((item) => {
          const Icon = icons[item.kind]
          return (
            <div
              key={item.id}
              className={cn(
                'pointer-events-auto flex max-w-md items-center gap-3 rounded-md px-4 py-3 text-sm font-medium text-white shadow-lg',
                item.kind === 'error' ? 'bg-[#8e1c1c]' : 'bg-[#202B29]',
              )}
              style={{ animation: 'kk-toast-in 160ms ease-out' }}
            >
              <Icon size={18} aria-hidden className={item.kind === 'success' ? 'text-[#4FC3A1]' : undefined} />
              <span className="flex-1">{item.message}</span>
              {item.action && (
                <button
                  type="button"
                  className="rounded px-2 py-1 font-semibold text-[#F5A623] hover:bg-white/10"
                  onClick={() => {
                    item.action?.onClick()
                    dismiss(item.id)
                  }}
                >
                  {item.action.label}
                </button>
              )}
              <button type="button" aria-label={t('common.dismiss')} className="rounded p-1 opacity-70 hover:opacity-100" onClick={() => dismiss(item.id)}>
                <X size={16} aria-hidden />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}
