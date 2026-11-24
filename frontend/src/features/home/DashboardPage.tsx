import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { BookOpen, Bookmark, Bot, GraduationCap, LifeBuoy, PenLine, UsersRound, Wrench } from 'lucide-react'
import { mentorshipApi, notificationsApi, opportunitiesApi, storiesApi } from '@/api/endpoints'
import { ButtonLink } from '@/components/ui/Button'
import { PhotoBackdrop } from '@/components/ui/PhotoBackdrop'
import { CardSkeletons, ErrorState, Skeleton } from '@/components/ui/feedback'
import { useAuth } from '@/features/auth/auth-context'
import { OpportunityCard } from '@/features/opportunities/OpportunityCard'
import { useToggleOpportunityBookmark } from '@/features/opportunities/hooks'
import { StoryCard } from '@/features/stories/StoryCard'
import { useDocumentTitle } from '@/lib/hooks'

function Stat({ label, value, to, loading }: { label: string; value?: number; to: string; loading: boolean }) {
  return (
    <Link to={to} className="card card-interactive flex flex-col justify-between gap-4 border-0 p-5">
      <span className="text-xs tracking-[0.06em] text-muted uppercase">{label}</span>
      {loading ? <Skeleton className="h-10 w-12" /> : <span className="text-[44px] leading-none font-light">{value ?? '—'}</span>}
    </Link>
  )
}

export default function DashboardPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  useDocumentTitle(t('nav.dashboard'))
  const firstName = user?.full_name.split(' ')[0] ?? ''

  const saved = useQuery({ queryKey: ['opportunities', 'dash-saved'], queryFn: () => opportunitiesApi.list({ bookmarked: true, page_size: 1 }) })
  const mine = useQuery({ queryKey: ['stories', 'dash-mine'], queryFn: () => storiesApi.list({ mine: true, page_size: 1 }) })
  const mentorship = useQuery({ queryKey: ['mentorship', 'dash'], queryFn: () => mentorshipApi.list({ page_size: 1 }) })
  const unread = useQuery({ queryKey: ['notifications', 'unread-count'], queryFn: notificationsApi.unreadCount })
  const featured = useQuery({ queryKey: ['opportunities', 'dash-featured'], queryFn: () => opportunitiesApi.list({ open: true, ordering: 'deadline', page_size: 3 }) })
  const stories = useQuery({ queryKey: ['stories', 'dash-latest'], queryFn: () => storiesApi.list({ ordering: '-published_at', page_size: 3 }) })
  const toggle = useToggleOpportunityBookmark()

  const needs = [
    { icon: GraduationCap, title: t('dashboard.needs.education'), body: t('dashboard.needs.educationBody'), to: '/opportunities?category=scholarship' },
    { icon: Wrench, title: t('dashboard.needs.work'), body: t('dashboard.needs.workBody'), to: '/opportunities?category=job' },
    { icon: LifeBuoy, title: t('dashboard.needs.services'), body: t('dashboard.needs.servicesBody'), to: '/services' },
    { icon: UsersRound, title: t('dashboard.needs.mentorship'), body: t('dashboard.needs.mentorshipBody'), to: '/alumni?mentors=1' },
  ]

  return (
    <div className="space-y-10">
      <header>
        <h1 className="text-[34px] font-light sm:text-[44px]">{t('dashboard.welcome', { name: firstName })}</h1>
        <p className="mt-2 text-muted">{t('dashboard.subtitle')}</p>
      </header>

      <div className="grid gap-px border border-border bg-border lg:grid-cols-[1fr_360px]">
        <section className="hero-gradient relative flex min-h-80 flex-col justify-end gap-5 overflow-hidden p-8 text-white sm:p-10">
          <PhotoBackdrop slot="dashboard" priority />
          <p className="eyebrow relative z-10 text-shell-accent">{t('dashboard.heroEyebrow')}</p>
          <h2 className="relative z-10 max-w-lg text-[30px] leading-tight font-light text-white sm:text-[38px]">{t('dashboard.heroTitle')}</h2>
          <div className="relative z-10 flex flex-wrap gap-3">
            <ButtonLink to="/opportunities" variant="inverse">
              {t('dashboard.exploreNow')}
            </ButtonLink>
            <ButtonLink to="/assistant" className="border border-white/70 text-white hover:bg-white hover:text-[#161616]" icon={<Bot size={18} aria-hidden />}>
              {t('dashboard.askAssistant')}
            </ButtonLink>
          </div>
        </section>
        <section aria-labelledby="community-h" className="stagger grid grid-cols-2 gap-px">
          <h2 id="community-h" className="sr-only">
            {t('dashboard.yourActivity')}
          </h2>
          <Stat label={t('dashboard.stats.saved')} value={saved.data?.count} to="/opportunities?saved=1" loading={saved.isPending} />
          <Stat label={t('dashboard.stats.stories')} value={mine.data?.count} to="/my-stories" loading={mine.isPending} />
          <Stat label={t('dashboard.stats.mentorship')} value={mentorship.data?.count} to="/mentorship" loading={mentorship.isPending} />
          <Stat label={t('dashboard.stats.unread')} value={unread.data?.count} to="/notifications" loading={unread.isPending} />
        </section>
      </div>

      <section aria-labelledby="needs-h" className="space-y-4">
        <h2 id="needs-h" className="text-[28px] font-light">
          {t('dashboard.exploreByNeed')}
        </h2>
        <div className="stagger grid gap-px border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
          {needs.map((n) => (
            <Link key={n.to} to={n.to} className="card card-interactive flex min-h-48 flex-col gap-2 border-0 p-6">
              <span className="icon-tile">
                <n.icon aria-hidden />
              </span>
              <h3 className="mt-auto text-lg font-normal">{n.title}</h3>
              <p className="text-sm text-muted">{n.body}</p>
            </Link>
          ))}
        </div>
      </section>

      <section aria-labelledby="featured-h" className="space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="featured-h" className="text-[28px] font-light">
            {t('dashboard.closingSoon')}
          </h2>
          <Link to="/opportunities" className="font-semibold text-primary hover:underline">
            {t('common.seeAll')}
          </Link>
        </div>
        {featured.isPending ? (
          <CardSkeletons count={3} />
        ) : featured.isError ? (
          <ErrorState compact error={featured.error} onRetry={() => void featured.refetch()} />
        ) : (
          <div className="grid gap-4">
            {featured.data.results.map((o) => (
              <OpportunityCard key={o.id} item={o} onToggleBookmark={(it) => toggle.mutate({ id: it.id, on: !it.is_bookmarked })} />
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="stories-h" className="space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="stories-h" className="text-[28px] font-light">
            {t('dashboard.storiesTitle')}
          </h2>
          <ButtonLink to="/stories/new" variant="text" icon={<PenLine size={18} aria-hidden />}>
            {t('stories.shareYourStory')}
          </ButtonLink>
        </div>
        {stories.isPending ? (
          <CardSkeletons count={3} className="grid gap-4 md:grid-cols-3" />
        ) : stories.isError ? (
          <ErrorState compact error={stories.error} onRetry={() => void stories.refetch()} />
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {stories.data.results.map((s) => (
              <StoryCard key={s.id} story={s} horizontal />
            ))}
          </div>
        )}
        <div className="flex flex-wrap gap-4 text-sm">
          <Link to="/my-stories?tab=saved" className="inline-flex items-center gap-2 font-semibold text-primary hover:underline">
            <Bookmark size={16} aria-hidden /> {t('stories.tabs.saved')}
          </Link>
          <Link to="/stories" className="inline-flex items-center gap-2 font-semibold text-primary hover:underline">
            <BookOpen size={16} aria-hidden /> {t('stories.browse')}
          </Link>
        </div>
      </section>
    </div>
  )
}
