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
    <div className="space-y-14">
      <section className="hero-gradient relative overflow-hidden rounded-xl px-6 py-12 text-white sm:px-12 sm:py-16">
        <div aria-hidden className="absolute -end-24 -top-24 size-72 rounded-full bg-white/10" />
        <div aria-hidden className="absolute -end-10 bottom-[-6rem] size-56 rounded-full bg-[#F5A623]/25" />
        <div className="relative max-w-2xl space-y-5">
          <p className="text-sm font-bold tracking-[0.12em] uppercase opacity-90">{t('landing.eyebrow')}</p>
          <h1 className="text-[36px] leading-tight font-bold text-white sm:text-[52px]">{t('landing.headline')}</h1>
          <p className="text-lg text-white/90">{t('landing.lead')}</p>
          <div className="flex flex-wrap gap-3 pt-2">
            <ButtonLink to="/opportunities" size="lg" className="bg-[#05483b] text-white hover:bg-[#043a30]">
              {t('landing.ctaExplore')}
            </ButtonLink>
            <ButtonLink to="/login?next=%2Fstories%2Fnew" variant="inverse" size="lg">
              {t('stories.shareYourStory')}
            </ButtonLink>
          </div>
        </div>
      </section>

      <section aria-labelledby="pillars-h" className="space-y-6">
        <h2 id="pillars-h" className="text-[26px] font-bold sm:text-[32px]">
          {t('landing.pillarsTitle')}
        </h2>
        <div className="grid gap-5 md:grid-cols-3">
          {pillars.map((p) => (
            <Link key={p.to} to={p.to} className="card group flex flex-col gap-3 p-7 hover:shadow-md">
              <p.icon className="text-accent" aria-hidden />
              <h3 className="text-xl font-semibold">{p.title}</h3>
              <p className="text-muted">{p.body}</p>
              <span className="mt-auto inline-flex items-center gap-1 pt-3 font-semibold text-primary">
                {t('landing.learnMore')} <ArrowRight size={16} aria-hidden className="transition-transform group-hover:translate-x-1 rtl:rotate-180" />
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section aria-labelledby="opps-h" className="space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="opps-h" className="text-2xl font-bold">
            {t('landing.openNow')}
          </h2>
          <Link to="/opportunities" className="font-semibold text-primary hover:underline">
            {t('common.seeAll')}
          </Link>
        </div>
        {opportunities.isPending ? (
          <CardSkeletons count={3} className="grid gap-4 md:grid-cols-3" />
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {opportunities.data?.results.map((o) => (
              <Link key={o.id} to={`/opportunities/${o.id}`} className="card flex flex-col gap-2 p-5 hover:shadow-md">
                <div className="flex flex-wrap gap-2">
                  <Badge>{t(`categories.opportunity.${o.category}`)}</Badge>
                  <DeadlineChip deadline={o.deadline} />
                </div>
                <h3 className="text-lg font-semibold">{o.title}</h3>
                <p className="text-sm text-muted">{o.organization}</p>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="stories-h" className="space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="stories-h" className="text-2xl font-bold">
            {t('landing.storiesTitle')}
          </h2>
          <Link to="/stories" className="font-semibold text-primary hover:underline">
            {t('common.seeAll')}
          </Link>
        </div>
        {stories.isPending ? (
          <CardSkeletons count={3} className="grid gap-5 md:grid-cols-3" />
        ) : (
          <div className="grid gap-5 md:grid-cols-3">
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
          <div key={f.title} className="flex gap-4 rounded-lg bg-primary-container p-6">
            <f.icon className="shrink-0 text-primary" aria-hidden />
            <div>
              <h3 className="font-semibold">{f.title}</h3>
              <p className="mt-1 text-sm text-muted">{f.body}</p>
            </div>
          </div>
        ))}
      </section>

      <section className="card flex flex-col items-center gap-4 p-10 text-center">
        <Users className="text-accent" size={36} aria-hidden />
        <h2 className="text-2xl font-bold">{t('landing.joinTitle')}</h2>
        <p className="max-w-xl text-muted">{t('landing.joinBody')}</p>
        <div className="flex flex-wrap justify-center gap-3">
          <ButtonLink to="/register" size="lg">
            {t('auth.joinUs')}
          </ButtonLink>
          <ButtonLink to="/login" variant="secondary" size="lg">
            {t('auth.signIn')}
          </ButtonLink>
        </div>
      </section>
    </div>
  )
}
