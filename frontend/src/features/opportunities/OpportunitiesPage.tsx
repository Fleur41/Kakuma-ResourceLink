import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { SearchX } from 'lucide-react'
import type { Opportunity, OpportunityCategory } from '@/api/types'
import { Chip } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { CardSkeletons, EmptyState, ErrorState } from '@/components/ui/feedback'
import { Checkbox } from '@/components/ui/Field'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader, SearchInput } from '@/components/ui/Page'
import { useAuth } from '@/features/auth/auth-context'
import { flatten } from '@/lib/cache'
import { useDebounced } from '@/lib/hooks'
import { useRequireMember } from '@/lib/useRequireMember'
import { useOpportunities, useToggleOpportunityBookmark } from './hooks'
import { OpportunityCard } from './OpportunityCard'

export const OPPORTUNITY_CATEGORIES: OpportunityCategory[] = ['scholarship', 'education', 'vocational', 'job', 'internship', 'entrepreneurship']

export default function OpportunitiesPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const requireMember = useRequireMember()
  // Filters live in the URL so results can be shared and survive reloads/back navigation.
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const category = params.get('category') ?? ''
  const remote = params.get('remote') === '1'
  const open = params.get('open') !== '0'
  const saved = params.get('saved') === '1'
  const debouncedSearch = useDebounced(search)

  const update = (patch: Record<string, string | null>) =>
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        for (const [k, v] of Object.entries(patch)) {
          if (v === null || v === '') next.delete(k)
          else next.set(k, v)
        }
        return next
      },
      { replace: true },
    )

  const filters = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      category: category || undefined,
      is_remote: remote ? true : undefined,
      open: open ? true : undefined,
      bookmarked: saved && user ? true : undefined,
      ordering: 'deadline',
    }),
    [debouncedSearch, category, remote, open, saved, user],
  )

  const query = useOpportunities(filters)
  const toggle = useToggleOpportunityBookmark()
  const items = flatten(query.data)
  const hasFilters = Boolean(search || category || remote || saved || !open)

  const onToggle = (o: Opportunity) => requireMember(() => toggle.mutate({ id: o.id, on: !o.is_bookmarked }))

  return (
    <>
      <PageHeader title={t('opportunities.title')} subtitle={t('opportunities.subtitle')} />
      <div className="mb-6">
        <SearchInput label={t('opportunities.searchLabel')} value={search} onChange={(v) => update({ search: v })} placeholder={t('opportunities.searchPlaceholder')} />
      </div>
      <div className="mb-6 flex gap-2 overflow-x-auto pb-1 lg:hidden" role="group" aria-label={t('opportunities.category')}>
        <Chip selected={!category} onClick={() => update({ category: null })}>
          {t('common.all')}
        </Chip>
        {OPPORTUNITY_CATEGORIES.map((c) => (
          <Chip key={c} selected={category === c} onClick={() => update({ category: category === c ? null : c })}>
            {t(`categories.opportunity.${c}`)}
          </Chip>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <aside className="card hidden h-fit space-y-6 p-6 lg:block" aria-labelledby="filters-title">
          <h2 id="filters-title" className="text-lg font-semibold">
            {t('opportunities.filterTitle')}
          </h2>
          <fieldset className="space-y-1">
            <legend className="mb-2 text-sm font-bold">{t('opportunities.category')}</legend>
            {['', ...OPPORTUNITY_CATEGORIES].map((c) => (
              <label key={c || 'all'} className="flex min-h-10 cursor-pointer items-center gap-3 rounded-md px-2 hover:bg-primary-container">
                <input type="radio" name="category" checked={category === c} onChange={() => update({ category: c || null })} className="size-4 accent-[var(--kk-primary)]" />
                {c ? t(`categories.opportunity.${c}`) : t('common.all')}
              </label>
            ))}
          </fieldset>
          <fieldset className="space-y-3">
            <legend className="mb-2 text-sm font-bold">{t('opportunities.more')}</legend>
            <Checkbox label={t('opportunities.remoteOnly')} checked={remote} onChange={(e) => update({ remote: e.target.checked ? '1' : null })} />
            <Checkbox label={t('opportunities.openOnly')} checked={open} onChange={(e) => update({ open: e.target.checked ? null : '0' })} />
            {user && <Checkbox label={t('opportunities.savedOnly')} checked={saved} onChange={(e) => update({ saved: e.target.checked ? '1' : null })} />}
          </fieldset>
          {hasFilters && (
            <Button variant="text" size="sm" onClick={() => setParams({}, { replace: true })}>
              {t('common.clearFilters')}
            </Button>
          )}
        </aside>

        <section aria-label={t('opportunities.results')} aria-busy={query.isFetching}>
          {query.isPending ? (
            <CardSkeletons count={4} />
          ) : query.isError && !items.length ? (
            <ErrorState error={query.error} onRetry={() => void query.refetch()} />
          ) : items.length === 0 ? (
            <EmptyState
              icon={<SearchX aria-hidden />}
              title={saved ? t('opportunities.noSaved') : t('opportunities.empty')}
              body={t('opportunities.emptyBody')}
              action={
                hasFilters && (
                  <Button variant="secondary" onClick={() => setParams({}, { replace: true })}>
                    {t('common.clearFilters')}
                  </Button>
                )
              }
            />
          ) : (
            <>
              <p className="mb-3 text-sm text-muted" aria-live="polite">
                {t('common.resultsCount', { count: query.data?.pages[0]?.count ?? items.length })}
              </p>
              <div className="grid gap-4">
                {items.map((o) => (
                  <OpportunityCard key={o.id} item={o} onToggleBookmark={onToggle} />
                ))}
              </div>
              <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
            </>
          )}
        </section>
      </div>
    </>
  )
}
