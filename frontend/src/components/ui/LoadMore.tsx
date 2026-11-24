import { useTranslation } from 'react-i18next'
import { Button } from './Button'

export function LoadMore({ hasNext, loading, onClick }: { hasNext?: boolean; loading: boolean; onClick: () => void }) {
  const { t } = useTranslation()
  if (!hasNext) return null
  return (
    <div className="mt-6 flex justify-center">
      <Button variant="secondary" loading={loading} onClick={onClick}>
        {t('common.loadMore')}
      </Button>
    </div>
  )
}
