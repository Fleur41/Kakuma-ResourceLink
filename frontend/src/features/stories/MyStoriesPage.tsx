import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { Bookmark, PenLine, Trash2 } from 'lucide-react'
import { storiesApi } from '@/api/endpoints'
import type { StoryList } from '@/api/types'
import { StatusBadge } from '@/components/ui/Badge'
import { ButtonLink, IconButton } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { CardSkeletons, EmptyState, ErrorState, useErrorMessage } from '@/components/ui/feedback'
import { LoadMore } from '@/components/ui/LoadMore'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { flatten } from '@/lib/cache'
import { cn, formatRelative } from '@/lib/format'
import { useStories } from './hooks'
import { StoryCard } from './StoryCard'

export default function MyStoriesPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'saved' ? 'saved' : 'mine'

  return (
    <>
      <PageHeader
        title={t('stories.myStories')}
        subtitle={t('stories.myStoriesSubtitle')}
        actions={
          <ButtonLink to="/stories/new" icon={<PenLine size={18} aria-hidden />}>
            {t('stories.writeStory')}
          </ButtonLink>
        }
      />
      <div role="tablist" className="mb-6 flex gap-2 border-b border-border">
        {(['mine', 'saved'] as const).map((k) => (
          <button
            key={k}
            role="tab"
            type="button"
            aria-selected={tab === k}
            onClick={() => setParams(k === 'mine' ? {} : { tab: k }, { replace: true })}
            className={cn('-mb-px min-h-11 border-b-2 px-4 font-semibold', tab === k ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-primary')}
          >
            {t(`stories.tabs.${k}`)}
          </button>
        ))}
      </div>
      {tab === 'mine' ? <MineList /> : <SavedList />}
    </>
  )
}

function MineList() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const query = useStories({ mine: true, ordering: '-created_at' }, 20)
  const items = flatten(query.data)
  const [toDelete, setToDelete] = useState<StoryList | null>(null)

  const remove = useMutation({
    mutationFn: (id: string) => storiesApi.remove(id),
    onSuccess: () => {
      setToDelete(null)
      toast.success(t('stories.deleted'))
      void qc.invalidateQueries({ queryKey: ['stories'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (query.isPending) return <CardSkeletons count={3} />
  if (query.isError && !items.length) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  if (!items.length)
    return <EmptyState icon={<PenLine aria-hidden />} title={t('stories.noOwn')} body={t('stories.noOwnBody')} action={<ButtonLink to="/stories/new">{t('stories.writeStory')}</ButtonLink>} />

  return (
    <>
      <ul className="space-y-3">
        {items.map((s) => (
          <li key={s.id} className="card flex flex-wrap items-center gap-4 p-4 sm:p-5">
            <div className="min-w-0 flex-1 space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <StatusBadge status={s.status} />
                <span className="text-sm text-muted">{t('stories.updated', { when: formatRelative(s.updated_at) })}</span>
              </div>
              <h2 className="truncate text-lg font-semibold">
                <Link to={`/stories/${s.id}`} className="hover:underline">
                  {s.title}
                </Link>
              </h2>
            </div>
            <div className="flex gap-1">
              <ButtonLink to={`/stories/${s.id}/edit`} variant="text" size="sm" icon={<PenLine size={16} aria-hidden />}>
                {t('stories.edit')}
              </ButtonLink>
              <IconButton label={t('stories.delete')} onClick={() => setToDelete(s)}>
                <Trash2 size={18} aria-hidden />
              </IconButton>
            </div>
          </li>
        ))}
      </ul>
      <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
      <ConfirmDialog
        open={toDelete !== null}
        title={t('stories.deleteTitle')}
        body={t('stories.deleteBody', { title: toDelete?.title ?? '' })}
        confirmLabel={t('common.delete')}
        destructive
        loading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
        onClose={() => setToDelete(null)}
      />
    </>
  )
}

function SavedList() {
  const { t } = useTranslation()
  const query = useStories({ bookmarked: true }, 20)
  const items = flatten(query.data)
  if (query.isPending) return <CardSkeletons count={3} />
  if (query.isError && !items.length) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  if (!items.length) return <EmptyState icon={<Bookmark aria-hidden />} title={t('stories.noSaved')} body={t('stories.noSavedBody')} action={<ButtonLink to="/stories">{t('stories.browse')}</ButtonLink>} />
  return (
    <>
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((s) => (
          <StoryCard key={s.id} story={s} />
        ))}
      </div>
      <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
    </>
  )
}
