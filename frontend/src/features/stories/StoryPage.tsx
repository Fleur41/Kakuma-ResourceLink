import { useState, type FormEvent } from 'react'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { ArrowLeft, Bookmark, BookmarkCheck, Bot, Flag, Heart, Link2, PenLine, Trash2 } from 'lucide-react'
import { nextPage } from '@/api/client'
import { storiesApi } from '@/api/endpoints'
import type { Comment } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge, StatusBadge } from '@/components/ui/Badge'
import { Button, ButtonLink, IconButton } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { ErrorState, PageLoader, useErrorMessage } from '@/components/ui/feedback'
import { TextArea } from '@/components/ui/Field'
import { LoadMore } from '@/components/ui/LoadMore'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { flatten } from '@/lib/cache'
import { formatDate, formatRelative } from '@/lib/format'
import { useDocumentTitle } from '@/lib/hooks'
import { useRequireMember } from '@/lib/useRequireMember'
import { useStory, useToggleLike, useToggleStoryBookmark } from './hooks'
import { Markdown } from './Markdown'
import { ReportDialog } from './ReportDialog'
import { StoryCover } from './StoryCard'

export default function StoryPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const { user } = useAuth()
  const toast = useToast()
  const requireMember = useRequireMember()
  const query = useStory(id)
  const like = useToggleLike()
  const bookmark = useToggleStoryBookmark()
  const [reportOpen, setReportOpen] = useState(false)
  useDocumentTitle(query.data?.title)

  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const s = query.data
  const isAuthor = user?.id === s.author.id
  const published = s.status === 'published'

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(window.location.href)
      toast.success(t('stories.linkCopied'))
    } catch {
      toast.error(t('errors.generic'))
    }
  }

  return (
    <article className="mx-auto max-w-[760px] space-y-6">
      <Link to="/stories" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary hover:underline">
        <ArrowLeft size={18} aria-hidden />
        {t('stories.back')}
      </Link>

      {isAuthor && !published && (
        <div className="card space-y-2 border-gold p-5" role="status">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={s.status} />
            <p className="font-medium">{t(`stories.statusHelp.${s.status}`, { defaultValue: '' })}</p>
          </div>
          {s.status === 'rejected' && s.rejection_reason && (
            <p className="text-sm">
              <span className="font-semibold">{t('stories.rejectionReason')}:</span> {s.rejection_reason}
            </p>
          )}
          <ButtonLink to={`/stories/${s.id}/edit`} variant="secondary" size="sm" icon={<PenLine size={16} aria-hidden />}>
            {t('stories.edit')}
          </ButtonLink>
        </div>
      )}

      <header className="space-y-4">
        <Badge tone="gold">{t(`categories.story.${s.category ?? 'inspiration'}`)}</Badge>
        <h1 className="text-[30px] leading-tight font-bold sm:text-[38px]">{s.title}</h1>
        <div className="flex items-center gap-3">
          <Avatar name={s.author.full_name} src={s.author.avatar} size={44} />
          <div>
            <p className="font-semibold">{s.author.full_name}</p>
            <p className="text-sm text-muted">
              {[s.author.country, s.published_at && formatDate(s.published_at), t('stories.readingTime', { count: s.reading_time })].filter(Boolean).join(' · ')}
            </p>
          </div>
        </div>
      </header>

      {s.cover_image && <StoryCover story={s} className="aspect-[16/8] w-full rounded-xl" />}

      <Markdown>{s.body}</Markdown>

      {published && (
        <div className="flex flex-wrap items-center gap-2 border-y border-border py-3">
          <Button
            variant={s.is_liked ? 'tonal' : 'text'}
            aria-pressed={s.is_liked}
            icon={<Heart size={18} aria-hidden className={s.is_liked ? 'fill-error text-error' : ''} />}
            onClick={() => requireMember(() => like.mutate({ id: s.id, on: !s.is_liked }))}
          >
            {t('stories.likesCount', { count: s.likes_count })}
          </Button>
          <IconButton
            label={s.is_bookmarked ? t('stories.removeBookmark') : t('stories.bookmark')}
            pressed={s.is_bookmarked}
            onClick={() => requireMember(() => bookmark.mutate({ id: s.id, on: !s.is_bookmarked }))}
          >
            {s.is_bookmarked ? <BookmarkCheck className="text-primary" aria-hidden /> : <Bookmark aria-hidden />}
          </IconButton>
          <IconButton label={t('stories.copyLink')} onClick={() => void copyLink()}>
            <Link2 aria-hidden />
          </IconButton>
          <ButtonLink to={`/assistant?task=summarize_story&target=${s.id}`} variant="text" icon={<Bot size={18} aria-hidden />}>
            {t('stories.summarizeWithAi')}
          </ButtonLink>
          {!isAuthor && (
            <Button variant="text" className="ms-auto text-muted" icon={<Flag size={16} aria-hidden />} onClick={() => requireMember(() => setReportOpen(true))}>
              {t('report.action')}
            </Button>
          )}
        </div>
      )}

      {published && <Comments storyId={s.id} count={s.comments_count} />}
      <ReportDialog open={reportOpen} onClose={() => setReportOpen(false)} target="story" targetId={s.id} />
    </article>
  )
}

function Comments({ storyId, count }: { storyId: string; count: number }) {
  const { t } = useTranslation()
  const { user, isModerator } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [body, setBody] = useState('')
  const [error, setError] = useState<string>()
  const [toDelete, setToDelete] = useState<Comment | null>(null)

  const query = useInfiniteQuery({
    queryKey: ['stories', 'comments', storyId],
    queryFn: ({ pageParam }) => storiesApi.comments(storyId, pageParam),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
  const comments = flatten(query.data)

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['stories', 'comments', storyId] })
    void qc.invalidateQueries({ queryKey: ['stories', 'detail', storyId] })
  }

  const add = useMutation({
    mutationFn: (text: string) => storiesApi.addComment(storyId, text),
    onSuccess: () => {
      setBody('')
      toast.success(t('stories.commentPosted'))
      refresh()
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: string) => storiesApi.deleteComment(id),
    onSuccess: () => {
      setToDelete(null)
      toast.success(t('stories.commentDeleted'))
      refresh()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const text = body.trim()
    if (!text) return setError(t('validation.required'))
    if (text.length > 2000) return setError(t('validation.maxLength', { count: 2000 }))
    setError(undefined)
    add.mutate(text)
  }

  return (
    <section aria-labelledby="comments-title" className="space-y-5">
      <h2 id="comments-title" className="text-xl font-semibold">
        {t('stories.commentsCount', { count })}
      </h2>
      {user ? (
        <form onSubmit={onSubmit} className="space-y-3">
          <TextArea label={t('stories.addComment')} value={body} onChange={(e) => setBody(e.target.value)} error={error} maxLength={2000} rows={3} />
          <Button type="submit" loading={add.isPending}>
            {t('stories.postComment')}
          </Button>
        </form>
      ) : (
        <p className="card p-4 text-muted">
          <Link to={`/login?next=${encodeURIComponent(`/stories/${storyId}`)}`} className="font-semibold text-primary hover:underline">
            {t('auth.signIn')}
          </Link>{' '}
          {t('stories.signInToComment')}
        </p>
      )}

      {query.isError && !comments.length ? (
        <ErrorState compact error={query.error} onRetry={() => void query.refetch()} />
      ) : (
        <ul className="space-y-4">
          {comments.map((c) => (
            <li key={c.id} className="flex gap-3">
              <Avatar name={c.author.full_name} src={c.author.avatar} size={36} />
              <div className="min-w-0 flex-1 rounded-lg bg-surface p-4 ring-1 ring-border">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm">
                    <span className="font-semibold">{c.author.full_name}</span> <span className="text-muted">· {formatRelative(c.created_at)}</span>
                  </p>
                  {(user?.id === c.author.id || isModerator) && (
                    <IconButton label={t('stories.deleteComment')} className="-me-2 -mt-2 size-9" onClick={() => setToDelete(c)}>
                      <Trash2 size={16} aria-hidden />
                    </IconButton>
                  )}
                </div>
                <p className="mt-1 whitespace-pre-line">{c.body}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
      <LoadMore hasNext={query.hasNextPage} loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()} />
      <ConfirmDialog
        open={toDelete !== null}
        title={t('stories.deleteCommentTitle')}
        body={t('stories.deleteCommentBody')}
        confirmLabel={t('common.delete')}
        destructive
        loading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
        onClose={() => setToDelete(null)}
      />
    </section>
  )
}
