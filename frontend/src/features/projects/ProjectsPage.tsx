import { useInfiniteQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { HandHeart, Plus } from 'lucide-react'
import { nextPage } from '@/api/client'
import { projectsApi } from '@/api/endpoints'
import type { Project, ProjectCategory } from '@/api/types'
import { Badge, Chip, StatusBadge } from '@/components/ui/Badge'
import { ButtonLink } from '@/components/ui/Button'
import { CardSkeletons, EmptyState, ErrorState } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader, SearchInput } from '@/components/ui/Page'
import { useAuth } from '@/features/auth/auth-context'
import { flatten } from '@/lib/cache'
import { formatAmount } from '@/lib/format'
import { useDebounced } from '@/lib/hooks'

export const PROJECT_CATEGORIES: ProjectCategory[] = ['education', 'health', 'livelihood', 'technology', 'environment', 'youth', 'other']

export function PledgeProgress({ project }: { project: Project }) {
  const { t } = useTranslation()
  const summary = project.contribution_summary
  const goal = project.funding_goal ? Number(project.funding_goal) : 0
  const pledged = Number(summary.pledged_amount) || 0
  const pct = goal > 0 ? Math.min(100, Math.round((pledged / goal) * 100)) : 0
  return (
    <div className="space-y-2">
      {goal > 0 && (
        <div className="h-2.5 overflow-hidden rounded-full bg-primary-container" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={pct} aria-label={t('projects.pledgedOfGoal')}>
          <div className="h-full rounded-full bg-gold" style={{ width: `${pct}%` }} />
        </div>
      )}
      <p className="text-sm text-muted">
        {goal > 0
          ? t('projects.pledgedAmountOfGoal', { pledged: formatAmount(pledged, summary.currency), goal: formatAmount(goal, project.currency ?? summary.currency) })
          : t('projects.pledgesCount', { count: summary.total_contributions })}
      </p>
    </div>
  )
}

export default function ProjectsPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const category = params.get('category') ?? ''
  const mine = params.get('mine') === '1' && Boolean(user)
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

  const filters = { search: debounced.trim() || undefined, category: category || undefined, mine: mine || undefined }
  const query = useInfiniteQuery({
    queryKey: ['projects', 'list', filters],
    queryFn: ({ pageParam }) => projectsApi.list({ ...filters, page: pageParam, page_size: 12 }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
  const items = flatten(query.data)

  return (
    <>
      <PageHeader
        title={t('projects.title')}
        subtitle={t('projects.subtitle')}
        actions={
          <ButtonLink to={user ? '/projects/new' : '/login?next=%2Fprojects%2Fnew'} icon={<Plus size={18} aria-hidden />}>
            {t('projects.propose')}
          </ButtonLink>
        }
      />
      <p className="card mb-6 border-gold p-4 text-sm">{t('projects.pledgeNotice')}</p>
      <div className="mb-4">
        <SearchInput label={t('projects.searchLabel')} value={search} onChange={(v) => set('search', v)} placeholder={t('projects.searchPlaceholder')} />
      </div>
      <div className="mb-6 flex gap-2 overflow-x-auto pb-1">
        <Chip selected={!category && !mine} onClick={() => setParams({}, { replace: true })}>
          {t('common.all')}
        </Chip>
        {user && (
          <Chip selected={mine} onClick={() => set('mine', mine ? '' : '1')}>
            {t('projects.mine')}
          </Chip>
        )}
        {PROJECT_CATEGORIES.map((c) => (
          <Chip key={c} selected={category === c} onClick={() => set('category', category === c ? '' : c)}>
            {t(`categories.project.${c}`)}
          </Chip>
        ))}
      </div>

      {query.isPending ? (
        <CardSkeletons count={4} className="grid gap-5 md:grid-cols-2" />
      ) : query.isError && !items.length ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<HandHeart aria-hidden />} title={t('projects.empty')} body={t('projects.emptyBody')} />
      ) : (
        <>
          <div className="grid gap-5 md:grid-cols-2">
            {items.map((p) => (
              <article key={p.id} className="card relative flex flex-col gap-3 overflow-hidden hover:shadow-md">
                {p.cover_image ? <img src={p.cover_image} alt="" loading="lazy" className="aspect-[16/7] w-full object-cover" /> : <div aria-hidden className="hero-gradient h-3" />}
                <div className="flex flex-1 flex-col gap-3 p-5 pt-2">
                  <div className="flex flex-wrap gap-2">
                    <Badge tone="gold">{t(`categories.project.${p.category ?? 'other'}`)}</Badge>
                    {p.status !== 'active' && <StatusBadge status={p.status} />}
                  </div>
                  <h2 className="text-lg font-semibold">
                    <Link to={`/projects/${p.id}`} className="after:absolute after:inset-0">
                      {p.title}
                    </Link>
                  </h2>
                  <p className="line-clamp-2 text-muted">{p.summary}</p>
                  <div className="mt-auto">
                    <PledgeProgress project={p} />
                  </div>
                </div>
              </article>
            ))}
          </div>
          <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
        </>
      )}
    </>
  )
}
