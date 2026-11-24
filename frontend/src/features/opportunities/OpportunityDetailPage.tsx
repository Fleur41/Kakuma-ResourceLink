import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { ArrowLeft, Bookmark, BookmarkCheck, Bot, Building2, CalendarClock, ExternalLink, Globe2, MapPin } from 'lucide-react'
import { Badge, VerifiedBadge } from '@/components/ui/Badge'
import { Button, ButtonLink, buttonClass } from '@/components/ui/Button'
import { ErrorState, PageLoader } from '@/components/ui/feedback'
import { formatDate } from '@/lib/format'
import { useDocumentTitle } from '@/lib/hooks'
import { useRequireMember } from '@/lib/useRequireMember'
import { useOpportunity, useToggleOpportunityBookmark } from './hooks'
import { DeadlineChip } from './OpportunityCard'

export default function OpportunityDetailPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const query = useOpportunity(id)
  const toggle = useToggleOpportunityBookmark()
  const requireMember = useRequireMember()
  useDocumentTitle(query.data?.title)

  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const o = query.data
  const safeUrl = o.application_url && /^https?:\/\//i.test(o.application_url) ? o.application_url : null

  return (
    <article className="space-y-6">
      <Link to="/opportunities" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary hover:underline">
        <ArrowLeft size={18} aria-hidden />
        {t('opportunities.back')}
      </Link>

      <header className="card space-y-4 p-6 sm:p-8">
        <div className="flex flex-wrap gap-2">
          <Badge>{t(`categories.opportunity.${o.category}`)}</Badge>
          <DeadlineChip deadline={o.deadline} />
          <VerifiedBadge verified={o.is_verified} />
        </div>
        <h1 className="text-[28px] leading-tight font-bold sm:text-[32px]">{o.title}</h1>
        <ul className="flex flex-wrap gap-x-6 gap-y-2 text-muted">
          <li className="flex items-center gap-2">
            <Building2 size={18} aria-hidden />
            {o.organization}
          </li>
          <li className="flex items-center gap-2">
            {o.is_remote ? <Globe2 size={18} aria-hidden /> : <MapPin size={18} aria-hidden />}
            {o.is_remote ? t('opportunities.remote') : o.location || '—'}
          </li>
          {o.deadline && (
            <li className="flex items-center gap-2">
              <CalendarClock size={18} aria-hidden />
              {t('opportunities.deadline', { date: formatDate(o.deadline, { dateStyle: 'long' }) })}
            </li>
          )}
        </ul>
        <div className="flex flex-wrap gap-3 pt-2">
          {safeUrl && (
            <a href={safeUrl} target="_blank" rel="noopener noreferrer" className={buttonClass('primary', 'lg')}>
              {t('opportunities.applyNow')}
              <ExternalLink size={18} aria-hidden />
              <span className="sr-only">{t('common.opensNewTab')}</span>
            </a>
          )}
          <Button
            variant="secondary"
            size="lg"
            icon={o.is_bookmarked ? <BookmarkCheck size={18} aria-hidden /> : <Bookmark size={18} aria-hidden />}
            aria-pressed={o.is_bookmarked}
            onClick={() => requireMember(() => toggle.mutate({ id: o.id, on: !o.is_bookmarked }))}
          >
            {o.is_bookmarked ? t('opportunities.savedLabel') : t('opportunities.save')}
          </Button>
          <ButtonLink to={`/assistant?task=explain_opportunity&target=${o.id}`} variant="tonal" size="lg" icon={<Bot size={18} aria-hidden />}>
            {t('opportunities.explainWithAi')}
          </ButtonLink>
        </div>
      </header>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <section className="card space-y-4 p-6 sm:p-8">
          <h2 className="text-xl font-semibold">{t('opportunities.about')}</h2>
          <p className="font-medium">{o.summary}</p>
          <p className="whitespace-pre-line">{o.description}</p>
        </section>
        <aside className="card h-fit space-y-3 p-6">
          <h2 className="text-lg font-semibold">{t('opportunities.requirements')}</h2>
          {o.requirements?.length ? (
            <ul className="list-disc space-y-2 ps-5">
              {o.requirements.map((r) => (
                <li key={r}>{r}</li>
              ))}
            </ul>
          ) : (
            <p className="text-muted">{t('opportunities.noRequirements')}</p>
          )}
          {!o.is_verified && <p className="border-t border-border pt-3 text-sm text-muted">{t('common.sampleContentNote')}</p>}
        </aside>
      </div>
    </article>
  )
}
