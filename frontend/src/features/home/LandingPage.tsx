import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { ArrowRight, Bot, BookOpen, Briefcase, HandHeart, Languages, LifeBuoy, Users } from 'lucide-react'
import { opportunitiesApi, storiesApi } from '@/api/endpoints'
import { ButtonLink } from '@/components/ui/Button'
import { CardSkeletons } from '@/components/ui/feedback'
import { StoryCard } from '@/features/stories/StoryCard'
import { useDocumentTitle } from '@/lib/hooks'
import { DeadlineChip } from '@/features/opportunities/OpportunityCard'
import { Badge } from '@/components/ui/Badge'
import { PhotoBackdrop } from '@/components/ui/PhotoBackdrop'

export default function LandingPage() {
  const { t } = useTranslation()
  useDocumentTitle(t('landing.title'))
  const stories = useQuery({ queryKey: ['stories', 'landing'], queryFn: () => storiesApi.list({ page_size: 3, ordering: '-published_at' }) })
  const opportunities = useQuery({ queryKey: ['opportunities', 'landing'], queryFn: () => opportunitiesApi.list({ page_size: 3, open: true, ordering: 'deadline' }) })

  const pillars = [
    { icon: Briefcase, title: t('landing.pillars.find.title'), body: t('landing.pillars.find.body'), to: '/opportunities' },
    { icon: BookOpen, title: t('landing.pillars.read.title'), body: t('landing.pillars.read.body'), to: '/stories' },
    { icon: HandHeart, title: t('landing.pillars.give.title'), body: t('landing.pillars.give.body'), to: '/alumni' },
  ]

  return (
    <div className="space-y-20">
      <section className="hero-gradient relative -mx-4 -mt-10 flex min-h-[600px] flex-col items-center justify-center overflow-hidden px-6 py-20 text-center text-white sm:-mx-6 lg:-mx-10 lg:-mt-14 lg:min-h-[680px]">
        <PhotoBackdrop slot="hero" priority />
        <div className="relative z-10 max-w-3xl space-y-6">
          <p className="eyebrow text-shell-accent">{t('landing.eyebrow')}</p>
          <h1 className="text-[40px] leading-[1.08] font-light text-white sm:text-[64px]">{t('landing.headline')}</h1>
          <p className="mx-auto max-w-xl text-lg font-light text-white/85">{t('landing.lead')}</p>
          <div className="flex flex-wrap justify-center gap-4 pt-4">
            <ButtonLink to="/opportunities" size="lg" variant="inverse">
              {t('landing.ctaExplore')}
            </ButtonLink>
            <ButtonLink to="/login?next=%2Fstories%2Fnew" size="lg" variant="outlineInverse">
              {t('stories.shareYourStory')}
            </ButtonLink>
          </div>
        </div>
      </section>

      <section aria-labelledby="pillars-h" className="space-y-6">
        <h2 id="pillars-h" className="text-[30px] font-light sm:text-[40px]">
          {t('landing.pillarsTitle')}
        </h2>
        <div className="stagger grid gap-px border border-border bg-border md:grid-cols-3">
          {pillars.map((p) => (
            <Link key={p.to} to={p.to} className="card card-interactive group flex min-h-64 flex-col gap-3 border-0 p-8">
              <span className="icon-tile">
                <p.icon aria-hidden />
              </span>
              <h3 className="text-xl font-normal">{p.title}</h3>
              <p className="text-muted">{p.body}</p>
              <span className="mt-auto inline-flex items-center gap-1 pt-3 text-sm font-medium text-primary">
                {t('landing.learnMore')} <ArrowRight size={16} aria-hidden className="transition-transform group-hover:translate-x-1 rtl:rotate-180" />
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section aria-labelledby="opps-h" className="space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="opps-h" className="text-[28px] font-light">
            {t('landing.openNow')}
          </h2>
          <Link to="/opportunities" className="font-semibold text-primary hover:underline">
            {t('common.seeAll')}
          </Link>
        </div>
        {opportunities.isPending ? (
          <CardSkeletons count={3} className="grid gap-4 md:grid-cols-3" />
        ) : (
          <div className="stagger grid gap-px border border-border bg-border md:grid-cols-3">
            {opportunities.data?.results.map((o) => (
              <Link key={o.id} to={`/opportunities/${o.id}`} className="card card-interactive flex flex-col gap-2 border-0 p-6">
                <div className="flex flex-wrap gap-2">
                  <Badge>{t(`categories.opportunity.${o.category}`)}</Badge>
                  <DeadlineChip deadline={o.deadline} />
                </div>
                <h3 className="text-lg font-normal">{o.title}</h3>
                <p className="text-sm text-muted">{o.organization}</p>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="stories-h" className="space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="stories-h" className="text-[28px] font-light">
            {t('landing.storiesTitle')}
          </h2>
          <Link to="/stories" className="font-semibold text-primary hover:underline">
            {t('common.seeAll')}
          </Link>
        </div>
        {stories.isPending ? (
          <CardSkeletons count={3} className="grid gap-5 md:grid-cols-3" />
        ) : (
          <div className="stagger grid gap-5 md:grid-cols-3">
            {stories.data?.results.map((s) => (
              <StoryCard key={s.id} story={s} />
            ))}
          </div>
        )}
      </section>

      <section className="grid gap-5 md:grid-cols-3">
        {[
          { icon: LifeBuoy, title: t('landing.features.services.title'), body: t('landing.features.services.body') },
          { icon: Bot, title: t('landing.features.ai.title'), body: t('landing.features.ai.body') },
          { icon: Languages, title: t('landing.features.languages.title'), body: t('landing.features.languages.body') },
        ].map((f) => (
          <div key={f.title} className="flex gap-4 border-t-2 border-primary pt-5">
            <f.icon size={22} aria-hidden className="mt-0.5 shrink-0 text-primary" />
            <div>
              <h3 className="font-semibold">{f.title}</h3>
              <p className="mt-1 text-sm text-muted">{f.body}</p>
            </div>
          </div>
        ))}
      </section>

      <section className="hero-gradient relative -mx-4 -mb-10 flex min-h-[480px] flex-col items-center justify-center gap-5 overflow-hidden px-6 py-24 text-center text-white sm:-mx-6 lg:-mx-10 lg:-mb-14">
        <PhotoBackdrop slot="community" />
        <Users size={28} aria-hidden className="relative z-10 text-shell-accent" />
        <h2 className="relative z-10 text-[32px] font-light text-white sm:text-[44px]">{t('landing.joinTitle')}</h2>
        <p className="relative z-10 max-w-xl font-light text-white/80">{t('landing.joinBody')}</p>
        <div className="relative z-10 flex flex-wrap justify-center gap-4 pt-2">
          <ButtonLink to="/register" size="lg" variant="inverse">
            {t('auth.joinUs')}
          </ButtonLink>
          <ButtonLink to="/login" size="lg" variant="outlineInverse">
            {t('auth.signIn')}
          </ButtonLink>
        </div>
      </section>
    </div>
  )
}
