import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { ArrowLeft, Clock, ExternalLink, Globe, Mail, MapPin, Phone } from 'lucide-react'
import { servicesApi } from '@/api/endpoints'
import { Badge, VerifiedBadge } from '@/components/ui/Badge'
import { buttonClass } from '@/components/ui/Button'
import { ErrorState, PageLoader } from '@/components/ui/feedback'
import { formatDate } from '@/lib/format'
import { useDocumentTitle } from '@/lib/hooks'

export default function ServiceDetailPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const query = useQuery({ queryKey: ['services', 'detail', id], queryFn: () => servicesApi.get(id) })
  useDocumentTitle(query.data?.name)

  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const s = query.data
  const website = s.website && /^https?:\/\//i.test(s.website) ? s.website : null
  const mapUrl = s.latitude && s.longitude ? `https://www.openstreetmap.org/?mlat=${s.latitude}&mlon=${s.longitude}#map=17/${s.latitude}/${s.longitude}` : null

  return (
    <article className="space-y-6">
      <Link to="/services" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary hover:underline">
        <ArrowLeft size={18} aria-hidden />
        {t('services.back')}
      </Link>
      <header className="card space-y-3 p-6 sm:p-8">
        <div className="flex flex-wrap gap-2">
          <Badge tone="gold">{t(`categories.service.${s.category}`)}</Badge>
          <VerifiedBadge verified={s.is_verified} />
        </div>
        <h1 className="text-[28px] font-bold">{s.name}</h1>
        {s.organization && <p className="text-muted">{s.organization}</p>}
        {s.last_verified_at && <p className="text-sm text-muted">{t('services.lastVerified', { date: formatDate(s.last_verified_at) })}</p>}
      </header>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <section className="card p-6 sm:p-8">
          <h2 className="mb-3 text-xl font-semibold">{t('services.about')}</h2>
          <p className="whitespace-pre-line">{s.description}</p>
        </section>
        <aside className="card h-fit space-y-4 p-6">
          <h2 className="text-lg font-semibold">{t('services.contact')}</h2>
          <ul className="space-y-3">
            {s.phone && (
              <li>
                <a href={`tel:${s.phone.replace(/\s+/g, '')}`} className="flex min-h-11 items-center gap-3 font-medium text-primary hover:underline">
                  <Phone size={18} aria-hidden /> {s.phone}
                </a>
              </li>
            )}
            {s.email && (
              <li>
                <a href={`mailto:${s.email}`} className="flex min-h-11 items-center gap-3 font-medium break-all text-primary hover:underline">
                  <Mail size={18} aria-hidden /> {s.email}
                </a>
              </li>
            )}
            {website && (
              <li>
                <a href={website} target="_blank" rel="noopener noreferrer" className="flex min-h-11 items-center gap-3 font-medium break-all text-primary hover:underline">
                  <Globe size={18} aria-hidden /> {website.replace(/^https?:\/\//, '')}
                  <span className="sr-only">{t('common.opensNewTab')}</span>
                </a>
              </li>
            )}
            {s.address && (
              <li className="flex items-start gap-3">
                <MapPin size={18} aria-hidden className="mt-0.5" /> {s.address}
              </li>
            )}
            {s.opening_hours && (
              <li className="flex items-start gap-3">
                <Clock size={18} aria-hidden className="mt-0.5" /> {s.opening_hours}
              </li>
            )}
          </ul>
          {mapUrl && (
            <a href={mapUrl} target="_blank" rel="noopener noreferrer" className={buttonClass('secondary', 'md', 'w-full')}>
              {t('services.openMap')} <ExternalLink size={16} aria-hidden />
            </a>
          )}
          {!s.phone && !s.email && !website && !s.address && <p className="text-muted">{t('services.noContact')}</p>}
        </aside>
      </div>
    </article>
  )
}
