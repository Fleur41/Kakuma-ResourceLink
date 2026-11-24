import { useInfiniteQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { MapPin, UserPlus, Users } from 'lucide-react'
import { nextPage } from '@/api/client'
import { alumniApi } from '@/api/endpoints'
import type { AlumniPublic } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge, Chip } from '@/components/ui/Badge'
import { ButtonLink } from '@/components/ui/Button'
import { CardSkeletons, EmptyState, ErrorState } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader, SearchInput } from '@/components/ui/Page'
import { useAuth } from '@/features/auth/auth-context'
import { flatten } from '@/lib/cache'
import { useDebounced } from '@/lib/hooks'

export default function AlumniPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const mentors = params.get('mentors') === '1'
  const skill = params.get('skill') ?? ''
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

  const filters = { search: debounced.trim() || undefined, is_mentor: mentors ? true : undefined, skill: skill || undefined }
  const query = useInfiniteQuery({
    queryKey: ['alumni', 'list', filters, Boolean(user)],
    queryFn: ({ pageParam }) => alumniApi.list({ ...filters, page: pageParam, page_size: 12 }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
  const items = flatten(query.data) as AlumniPublic[]

  return (
    <>
      <PageHeader
        title={t('alumni.title')}
        subtitle={t('alumni.subtitle')}
        actions={
          user && (
            <ButtonLink to="/alumni/me" variant="secondary" icon={<UserPlus size={18} aria-hidden />}>
              {user.has_alumni_profile ? t('alumni.editMyProfile') : t('alumni.joinDirectory')}
            </ButtonLink>
          )
        }
      />
      <div className="mb-4">
        <SearchInput label={t('alumni.searchLabel')} value={search} onChange={(v) => set('search', v)} placeholder={t('alumni.searchPlaceholder')} />
      </div>
      <div className="mb-6 flex flex-wrap gap-2">
        <Chip selected={!mentors && !skill} onClick={() => setParams({}, { replace: true })}>
          {t('alumni.all')}
        </Chip>
        <Chip selected={mentors} onClick={() => set('mentors', mentors ? '' : '1')}>
          {t('alumni.mentors')}
        </Chip>
        {skill && (
          <Chip selected onClick={() => set('skill', '')}>
            {skill} ✕
          </Chip>
        )}
      </div>
      {!user && <p className="mb-4 text-sm text-muted">{t('alumni.guestNote')}</p>}

      {query.isPending ? (
        <CardSkeletons count={6} className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3" />
      ) : query.isError && !items.length ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<Users aria-hidden />} title={t('alumni.empty')} body={t('alumni.emptyBody')} />
      ) : (
        <>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((a) => (
              <article key={a.id} className="card flex flex-col gap-3 p-6">
                <div className="flex items-center gap-3">
                  <Avatar name={a.full_name} src={a.avatar} size={56} />
                  <div className="min-w-0">
                    <h2 className="truncate text-lg font-semibold">{a.full_name}</h2>
                    <p className="truncate text-sm text-muted">{a.profession}</p>
                  </div>
                </div>
                <p className="flex items-center gap-2 text-sm text-muted">
                  <MapPin size={16} aria-hidden />
                  {[a.city, a.country].filter(Boolean).join(', ')}
                </p>
                {a.is_mentor && <Badge tone="gold">{t('alumni.openToMentor')}</Badge>}
                {a.skills.length > 0 && (
                  <ul className="flex flex-wrap gap-1.5" aria-label={t('alumni.skills')}>
                    {a.skills.slice(0, 4).map((s) => (
                      <li key={s}>
                        <button type="button" onClick={() => set('skill', s)} className="rounded-full bg-primary-container px-2.5 py-1 text-xs font-medium text-primary hover:underline">
                          {s}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <ButtonLink to={`/alumni/${a.id}`} variant="secondary" size="sm" className="mt-auto">
                  {t('alumni.viewProfile')}
                  <span className="sr-only">: {a.full_name}</span>
                </ButtonLink>
              </article>
            ))}
          </div>
          <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
        </>
      )}
      {user && (
        <p className="mt-8 text-center text-sm text-muted">
          <Link to="/mentorship" className="font-semibold text-primary hover:underline">
            {t('alumni.seeMyRequests')}
          </Link>
        </p>
      )}
    </>
  )
}
