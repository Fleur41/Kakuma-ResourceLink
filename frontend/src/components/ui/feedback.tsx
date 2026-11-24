import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, CloudOff, Inbox, RotateCw } from 'lucide-react'
import { ApiError } from '@/api/client'
import { cn } from '@/lib/format'

export function Spinner({ size = 20, className }: { size?: number; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn('inline-block animate-spin rounded-full border-2 border-current border-t-transparent', className)}
      style={{ width: size, height: size }}
    />
  )
}

export function PageLoader() {
  const { t } = useTranslation()
  return (
    <div role="status" className="flex min-h-[40vh] items-center justify-center text-primary">
      <Spinner size={32} />
      <span className="sr-only">{t('common.loading')}</span>
    </div>
  )
}

export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cn('skeleton', className)} />
}

/** Card-shaped placeholders while a list loads. */
export function CardSkeletons({ count = 3, className }: { count?: number; className?: string }) {
  const { t } = useTranslation()
  return (
    <div role="status" aria-label={t('common.loading')} className={className ?? 'grid gap-4'}>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="card space-y-3 p-5">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-6 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      ))}
    </div>
  )
}

interface EmptyStateProps {
  title: string
  body?: string
  icon?: ReactNode
  action?: ReactNode
}

export function EmptyState({ title, body, icon, action }: EmptyStateProps) {
  return (
    <div className="card flex flex-col items-center gap-3 px-6 py-12 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-primary-container text-primary">{icon ?? <Inbox aria-hidden />}</div>
      <h3 className="text-lg font-semibold">{title}</h3>
      {body && <p className="max-w-md text-muted">{body}</p>}
      {action}
    </div>
  )
}

/** Maps an error to a short localized message. Field validation details are shown by forms instead. */
export function useErrorMessage() {
  const { t } = useTranslation()
  return (error: unknown): string => {
    if (error instanceof ApiError) {
      if (error.isNetwork) return t('errors.network')
      switch (error.code) {
        case 'throttled':
          return t('errors.throttled')
        case 'not_found':
          return t('errors.notFound')
        case 'permission_denied':
          return t('errors.forbidden')
        case 'not_authenticated':
        case 'token_not_valid':
          return t('errors.signInAgain')
        case 'ai_unavailable':
          return t('assistant.unavailable')
        case 'validation_error':
        case 'conflict':
        case 'invalid_transition':
        case 'authentication_failed':
          return error.message
      }
      if (error.status >= 500) return t('errors.server')
      return error.message || t('errors.generic')
    }
    return t('errors.generic')
  }
}

export function ErrorState({ error, onRetry, compact }: { error: unknown; onRetry?: () => void; compact?: boolean }) {
  const { t } = useTranslation()
  const message = useErrorMessage()(error)
  const offline = error instanceof ApiError && error.isNetwork
  return (
    <div role="alert" className={cn('card flex flex-col items-center gap-3 text-center', compact ? 'p-5' : 'px-6 py-12')}>
      <div className="flex size-12 items-center justify-center rounded-full bg-gold-container text-gold-text">
        {offline ? <CloudOff aria-hidden /> : <AlertTriangle aria-hidden />}
      </div>
      <p className="font-medium">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="inline-flex min-h-11 items-center gap-2 rounded-md px-4 font-semibold text-primary hover:bg-primary-container">
          <RotateCw size={16} aria-hidden />
          {t('common.retry')}
        </button>
      )}
    </div>
  )
}

export function OfflineBanner() {
  const { t } = useTranslation()
  return (
    <div role="status" className="flex items-center justify-center gap-2 bg-gold px-4 py-2 text-center text-sm font-medium text-[#3d2800]">
      <CloudOff size={16} aria-hidden />
      {t('common.offlineBanner')}
    </div>
  )
}
