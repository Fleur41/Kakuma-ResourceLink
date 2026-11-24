import { useTranslation } from 'react-i18next'
import { useRouteError } from 'react-router'
import { ErrorState } from '@/components/ui/feedback'

/** Last-resort boundary (e.g. a lazy chunk failed to load after a deploy while offline). */
export default function RouteError() {
  const error = useRouteError()
  const { t } = useTranslation()
  if (import.meta.env.DEV) console.error(error)
  return (
    <main className="mx-auto max-w-xl p-8">
      <ErrorState error={error} onRetry={() => window.location.reload()} />
      <p className="mt-4 text-center text-sm text-muted">{t('errors.reloadHint')}</p>
    </main>
  )
}
