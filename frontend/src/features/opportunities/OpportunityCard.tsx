import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Bookmark, BookmarkCheck, CalendarClock } from 'lucide-react'
import type { Opportunity } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { IconButton } from '@/components/ui/Button'
import { daysUntil, formatDate } from '@/lib/format'

export function DeadlineChip({ deadline }: { deadline?: string | null }) {
  const { t } = useTranslation()
  if (!deadline) return <Badge tone="neutral">{t('opportunities.noDeadline')}</Badge>
  const days = daysUntil(deadline)
  if (days < 0) return <Badge tone="neutral">{t('opportunities.closed')}</Badge>
  const label = days <= 14 ? t('opportunities.daysLeft', { count: days }) : t('opportunities.deadline', { date: formatDate(deadline) })
  return (
    <Badge tone="gold">
      <CalendarClock size={14} aria-hidden />
      {label}
    </Badge>
  )
}

interface Props {
  item: Opportunity
  onToggleBookmark: (item: Opportunity) => void
}

export function OpportunityCard({ item, onToggleBookmark }: Props) {
  const { t } = useTranslation()
  return (
    <article className="card relative flex gap-4 p-5 transition-shadow focus-within:ring-2 focus-within:ring-accent/40 hover:shadow-md sm:p-6">
      <div className="min-w-0 flex-1 space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge>{t(`categories.opportunity.${item.category}`)}</Badge>
          <DeadlineChip deadline={item.deadline} />
        </div>
        <h3 className="text-lg font-semibold sm:text-xl">
          {/* Whole card is clickable via the stretched link; the bookmark button sits above it. */}
          <Link to={`/opportunities/${item.id}`} className="after:absolute after:inset-0 after:rounded-lg focus:outline-none">
            {item.title}
          </Link>
        </h3>
        <p className="text-sm text-muted">
          {[item.organization, item.is_remote ? t('opportunities.remote') : item.location].filter(Boolean).join(' · ')}
        </p>
        <p className="line-clamp-2 text-[15px]">{item.summary}</p>
      </div>
      <IconButton
        label={item.is_bookmarked ? t('opportunities.removeBookmark') : t('opportunities.bookmark')}
        pressed={item.is_bookmarked}
        onClick={() => onToggleBookmark(item)}
        className="relative z-10 -me-2 -mt-2"
      >
        {item.is_bookmarked ? <BookmarkCheck className="text-primary" aria-hidden /> : <Bookmark aria-hidden />}
      </IconButton>
    </article>
  )
}
