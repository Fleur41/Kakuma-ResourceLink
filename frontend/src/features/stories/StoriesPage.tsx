import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { BookOpen, PenLine } from 'lucide-react'
import { Chip } from '@/components/ui/Badge'
import { ButtonLink } from '@/components/ui/Button'
import { CardSkeletons, EmptyState, ErrorState } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader, SearchInput } from '@/components/ui/Page'
import { useAuth } from '@/features/auth/auth-context'
import { flatten } from '@/lib/cache'
import { cn } from '@/lib/format'
import { useDebounced } from '@/lib/hooks'
import { STORY_CATEGORIES, useStories } from './hooks'
import { StoryCard } from './StoryCard'

const SORTS = { latest: '-published_at', popular: '-likes_count' } as const

export default function StoriesPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const category = params.get('category') ?? ''
  const sort = (params.get('sort') as keyof typeof SORTS) in SORTS ? (params.get('sort') as keyof typeof SORTS) : 'latest'
  const debounced = useDebounced(search)

  const set = (k: string, v: string) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        if (v) n.set(k, v)
        else n.delete(k)
        return n
      },
      { replace: true },
    )

  const query = useStories({ search: debounced.trim() || undefined, category: category || undefined, ordering: SORTS[sort] })
  const stories = flatten(query.data)
  const writeHref = user ? '/stories/new' : '/login?next=%2Fstories%2Fnew'

  return (
    <>
      <PageHeader
        title={t('stories.title')}
        subtitle={t('stories.subtitle')}
        actions={
          <ButtonLink to={writeHref} icon={<PenLine size={18} aria-hidden />}>
            {t('stories.shareYourStory')}
          </ButtonLink>
        }
      />
      <div className="mb-4">
        <SearchInput label={t('stories.searchLabel')} value={search} onChange={(v) => set('search', v)} placeholder={t('stories.searchPlaceholder')} />
      </div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div role="tablist" aria-label={t('stories.sortLabel')} className="inline-flex rounded-full border border-border bg-surface p-1">
          {(Object.keys(SORTS) as (keyof typeof SORTS)[]).map((s) => (
            <button
              key={s}
              role="tab"
              type="button"
              aria-selected={sort === s}
              onClick={() => set('sort', s === 'latest' ? '' : s)}
              className={cn('min-h-10 rounded-full px-5 text-sm font-semibold', sort === s ? 'bg-primary text-on-primary' : 'text-muted hover:text-primary')}
            >
              {t(`stories.sort.${s}`)}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1fr_300px]">
        <section aria-label={t('stories.title')} aria-busy={query.isFetching}>
          {query.isPending ? (
            <CardSkeletons count={4} className="grid gap-5 sm:grid-cols-2" />
          ) : query.isError && !stories.length ? (
            <ErrorState error={query.error} onRetry={() => void query.refetch()} />
          ) : stories.length === 0 ? (
            <EmptyState
              icon={<BookOpen aria-hidden />}
              title={t('stories.empty')}
              body={t('stories.emptyBody')}
              action={<ButtonLink to={writeHref}>{t('stories.writeStory')}</ButtonLink>}
            />
          ) : (
            <>
              <div className="grid gap-5 sm:grid-cols-2">
                {stories.map((s) => (
                  <StoryCard key={s.id} story={s} />
                ))}
              </div>
              <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
            </>
          )}
        </section>

        <aside className="space-y-5">
          <div className="hero-gradient rounded-xl p-6 text-white">
            <h2 className="text-xl font-bold text-white">{t('stories.ctaTitle')}</h2>
            <p className="mt-2 text-white/90">{t('stories.ctaBody')}</p>
            <ButtonLink to={writeHref} variant="inverse" className="mt-4">
              {t('stories.writeStory')}
            </ButtonLink>
          </div>
          <div className="card p-6">
            <h2 className="mb-3 text-lg font-semibold">{t('stories.exploreTopics')}</h2>
            <div className="flex flex-wrap gap-2">
              <Chip selected={!category} onClick={() => set('category', '')}>
                {t('common.all')}
              </Chip>
              {STORY_CATEGORIES.map((c) => (
                <Chip key={c} selected={category === c} onClick={() => set('category', category === c ? '' : c)}>
                  {t(`categories.story.${c}`)}
                </Chip>
              ))}
            </div>
          </div>
          {user && (
            <ButtonLink to="/my-stories" variant="secondary" className="w-full">
              {t('stories.myStories')}
            </ButtonLink>
          )}
        </aside>
      </div>
    </>
  )
}
