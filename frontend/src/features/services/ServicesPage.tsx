import { useInfiniteQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { Clock, MapPin, Phone, SearchX } from 'lucide-react'
import { nextPage } from '@/api/client'
import { servicesApi } from '@/api/endpoints'
import type { ServiceCategory } from '@/api/types'
import { Badge, Chip, VerifiedBadge } from '@/components/ui/Badge'
import { CardSkeletons, EmptyState, ErrorState } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader, SearchInput } from '@/components/ui/Page'
import { flatten } from '@/lib/cache'
import { useDebounced } from '@/lib/hooks'

export const SERVICE_CATEGORIES: ServiceCategory[] = ['healthcare', 'humanitarian', 'education', 'legal', 'protection', 'livelihood', 'community']

export default function ServicesPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const category = params.get('category') ?? ''
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

  const filters = { search: debounced.trim() || undefined, category: category || undefined }
  const query = useInfiniteQuery({
    queryKey: ['services', 'list', filters],
    queryFn: ({ pageParam }) => servicesApi.list({ ...filters, page: pageParam, page_size: 20 }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
  const items = flatten(query.data)

  return (
    <>
      <PageHeader title={t('services.title')} subtitle={t('services.subtitle')} />
      <div className="mb-4">
        <SearchInput label={t('services.searchLabel')} value={search} onChange={(v) => set('search', v)} placeholder={t('services.searchPlaceholder')} />
      </div>
      <div className="mb-6 flex gap-2 overflow-x-auto pb-1" role="group" aria-label={t('services.category')}>
        <Chip selected={!category} onClick={() => set('category', '')}>
          {t('common.all')}
        </Chip>
        {SERVICE_CATEGORIES.map((c) => (
          <Chip key={c} selected={category === c} onClick={() => set('category', category === c ? '' : c)}>
            {t(`categories.service.${c}`)}
          </Chip>
        ))}
      </div>

      {query.isPending ? (
        <CardSkeletons count={4} className="grid gap-4 md:grid-cols-2" />
      ) : query.isError && !items.length ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<SearchX aria-hidden />} title={t('services.empty')} body={t('services.emptyBody')} />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            {items.map((s) => (
              <article key={s.id} className="card relative flex flex-col gap-3 p-5 hover:shadow-md">
                <div className="flex flex-wrap gap-2">
                  <Badge tone="gold">{t(`categories.service.${s.category}`)}</Badge>
                  <VerifiedBadge verified={s.is_verified} />
                </div>
                <h2 className="text-lg font-semibold">
                  <Link to={`/services/${s.id}`} className="after:absolute after:inset-0 after:rounded-lg">
                    {s.name}
                  </Link>
                </h2>
                {s.organization && <p className="-mt-2 text-sm text-muted">{s.organization}</p>}
                <p className="line-clamp-2">{s.description}</p>
                <ul className="mt-auto space-y-1 text-sm text-muted">
                  {s.address && (
                    <li className="flex items-center gap-2">
                      <MapPin size={16} aria-hidden /> {s.address}
                    </li>
                  )}
                  {s.opening_hours && (
                    <li className="flex items-center gap-2">
                      <Clock size={16} aria-hidden /> {s.opening_hours}
                    </li>
                  )}
                  {s.phone && (
                    <li className="flex items-center gap-2">
                      <Phone size={16} aria-hidden /> {s.phone}
                    </li>
                  )}
                </ul>
              </article>
            ))}
          </div>
          <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
        </>
      )}
    </>
  )
}
