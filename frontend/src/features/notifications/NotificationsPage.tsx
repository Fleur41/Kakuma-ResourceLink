import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Bell, BookOpen, CheckCheck, HandHeart, MessageCircle, UsersRound } from 'lucide-react'
import { nextPage } from '@/api/client'
import { notificationsApi } from '@/api/endpoints'
import type { Notification } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { CardSkeletons, EmptyState, ErrorState, useErrorMessage } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { flatten } from '@/lib/cache'
import { cn, formatRelative } from '@/lib/format'

/** Where a notification leads, from its type and data payload. */
export function notificationTarget(n: Pick<Notification, 'type' | 'data'>): string | null {
  const data = (n.data ?? {}) as Record<string, string | undefined>
  if (data.story_id) return `/stories/${data.story_id}`
  if (data.project_id) return `/projects/${data.project_id}`
  if (n.type === 'mentorship_request') return '/mentorship'
  if (n.type === 'mentorship_update') return '/mentorship?box=sent'
  return null
}

const ICONS: Partial<Record<Notification['type'], typeof Bell>> = {
  mentorship_request: UsersRound,
  mentorship_update: UsersRound,
  story_published: BookOpen,
  story_rejected: BookOpen,
  story_comment: MessageCircle,
  project_contribution: HandHeart,
  project_status: HandHeart,
}

export default function NotificationsPage() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const toast = useToast()
  const navigate = useNavigate()
  const errorMessage = useErrorMessage()
  const [params, setParams] = useSearchParams()
  const unreadOnly = params.get('filter') === 'unread'

  const query = useInfiniteQuery({
    queryKey: ['notifications', 'list', unreadOnly],
    queryFn: ({ pageParam }) => notificationsApi.list({ unread: unreadOnly || undefined, page: pageParam, page_size: 20 }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
  const items = flatten(query.data)

  const markRead = useMutation({
    mutationFn: notificationsApi.markRead,
    onSettled: () => void qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
  const markAll = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => {
      toast.success(t('notifications.allRead'))
      void qc.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  function open(n: Notification) {
    if (!n.is_read) markRead.mutate(n.id)
    const target = notificationTarget(n)
    if (target) navigate(target)
  }

  return (
    <>
      <PageHeader
        title={t('notifications.title')}
        actions={
          <Button variant="secondary" icon={<CheckCheck size={18} aria-hidden />} loading={markAll.isPending} onClick={() => markAll.mutate()} disabled={!items.some((n) => !n.is_read)}>
            {t('notifications.markAllRead')}
          </Button>
        }
      />
      <div role="tablist" className="mb-6 flex gap-2 border-b border-border">
        {(['all', 'unread'] as const).map((k) => (
          <button
            key={k}
            role="tab"
            type="button"
            aria-selected={(k === 'unread') === unreadOnly}
            onClick={() => setParams(k === 'all' ? {} : { filter: k }, { replace: true })}
            className={cn('-mb-px min-h-11 border-b-2 px-4 font-semibold', (k === 'unread') === unreadOnly ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-primary')}
          >
            {t(`notifications.tabs.${k}`)}
          </button>
        ))}
      </div>
      {query.isPending ? (
        <CardSkeletons count={4} />
      ) : query.isError && !items.length ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState icon={<Bell aria-hidden />} title={unreadOnly ? t('notifications.noUnread') : t('notifications.empty')} body={t('notifications.emptyBody')} />
      ) : (
        <>
          <ul className="card divide-y divide-border overflow-hidden">
            {items.map((n) => {
              const Icon = ICONS[n.type] ?? Bell
              return (
                <li key={n.id}>
                  <button type="button" onClick={() => open(n)} className={cn('flex w-full items-start gap-4 p-4 text-start hover:bg-primary-container/50 sm:p-5', !n.is_read && 'bg-primary-container/30')}>
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-container text-primary">
                      <Icon size={20} aria-hidden />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className={cn('block', !n.is_read && 'font-semibold')}>{n.title}</span>
                      {n.body && <span className="mt-0.5 block text-sm text-muted">{n.body}</span>}
                      <span className="mt-1 block text-xs text-muted">{formatRelative(n.created_at)}</span>
                    </span>
                    {!n.is_read && (
                      <span className="mt-2 size-2.5 shrink-0 rounded-full bg-gold">
                        <span className="sr-only">{t('notifications.unread')}</span>
                      </span>
                    )}
                  </button>
                </li>
              )
            })}
          </ul>
          <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
        </>
      )}
    </>
  )
}
