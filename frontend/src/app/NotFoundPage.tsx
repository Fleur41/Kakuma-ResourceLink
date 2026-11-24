import { useTranslation } from 'react-i18next'
import { Compass } from 'lucide-react'
import { ButtonLink } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/feedback'
import { useDocumentTitle } from '@/lib/hooks'

export default function NotFoundPage() {
  const { t } = useTranslation()
  useDocumentTitle(t('errors.pageNotFound'))
  return <EmptyState icon={<Compass aria-hidden />} title={t('errors.pageNotFound')} body={t('errors.pageNotFoundBody')} action={<ButtonLink to="/">{t('common.goHome')}</ButtonLink>} />
}
