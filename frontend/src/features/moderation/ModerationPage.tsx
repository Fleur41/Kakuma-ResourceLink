import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { ShieldCheck } from 'lucide-react'
import { moderationApi } from '@/api/endpoints'
import { Avatar } from '@/components/ui/Avatar'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { CardSkeletons, EmptyState, ErrorState, useErrorMessage } from '@/components/ui/feedback'
import { TextArea } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { Markdown } from '@/features/stories/Markdown'
import { cn, formatRelative } from '@/lib/format'

type Decision = { kind: 'story' | 'project'; id: string; approve: boolean; title: string }

export default function ModerationPage() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'projects' ? 'projects' : 'stories'
  const [decision, setDecision] = useState<Decision | null>(null)
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState<string>()

  const stories = useQuery({ queryKey: ['moderation', 'stories'], queryFn: moderationApi.stories, enabled: tab === 'stories' })
  const projects = useQuery({ queryKey: ['moderation', 'projects'], queryFn: moderationApi.projects, enabled: tab === 'projects' })

  const decide = useMutation({
    mutationFn: async (d: Decision): Promise<unknown> => {
      if (d.kind === 'project') return moderationApi.decideProject(d.id, d.approve, note.trim())
      return d.approve ? moderationApi.approveStory(d.id) : moderationApi.rejectStory(d.id, note.trim())
    },
    onSuccess: (_r, d) => {
      toast.success(d.approve ? t('moderation.approved') : t('moderation.rejected'))
      setDecision(null)
      setNote('')
      void qc.invalidateQueries({ queryKey: ['moderation'] })
      void qc.invalidateQueries({ queryKey: [d.kind === 'story' ? 'stories' : 'projects'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  function confirm() {
    if (!decision) return
    if (!decision.approve && decision.kind === 'story' && note.trim().length < 3) return setNoteError(t('moderation.reasonRequired'))
    setNoteError(undefined)
    decide.mutate(decision)
  }

  const active = tab === 'stories' ? stories : projects

  return (
    <>
      <PageHeader title={t('moderation.title')} subtitle={t('moderation.subtitle')} />
      <div role="tablist" className="mb-6 flex gap-2 border-b border-border">
        {(['stories', 'projects'] as const).map((k) => (
          <button
            key={k}
            role="tab"
            type="button"
            aria-selected={tab === k}
            onClick={() => setParams(k === 'stories' ? {} : { tab: k }, { replace: true })}
            className={cn('-mb-px min-h-11 border-b-2 px-4 font-semibold', tab === k ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-primary')}
          >
            {t(`moderation.tabs.${k}`)}
          </button>
        ))}
      </div>

      {active.isPending ? (
        <CardSkeletons count={2} />
      ) : active.isError ? (
        <ErrorState error={active.error} onRetry={() => void active.refetch()} />
      ) : (active.data?.results.length ?? 0) === 0 ? (
        <EmptyState icon={<ShieldCheck aria-hidden />} title={t('moderation.empty')} body={t('moderation.emptyBody')} />
      ) : tab === 'stories' ? (
        <ul className="space-y-4">
          {stories.data?.results.map((s) => (
            <li key={s.id} className="card space-y-4 p-5 sm:p-6">
              <div className="flex flex-wrap items-center gap-3">
                <Avatar name={s.author.full_name} src={s.author.avatar} size={36} />
                <div className="min-w-0 flex-1">
                  <h2 className="text-lg font-semibold">{s.title}</h2>
                  <p className="text-sm text-muted">
                    {s.author.full_name} · {formatRelative(s.updated_at)}
                  </p>
                </div>
                <Badge tone="gold">{t(`categories.story.${s.category ?? 'inspiration'}`)}</Badge>
              </div>
              <details className="rounded-md border border-border p-4">
                <summary className="cursor-pointer font-semibold text-primary">{t('moderation.readStory')}</summary>
                <div className="mt-3">
                  <Markdown>{s.body}</Markdown>
                </div>
              </details>
              <div className="flex flex-wrap gap-3">
                <Button size="sm" onClick={() => setDecision({ kind: 'story', id: s.id, approve: true, title: s.title })}>
                  {t('moderation.approve')}
                </Button>
                <Button size="sm" variant="secondary" onClick={() => setDecision({ kind: 'story', id: s.id, approve: false, title: s.title })}>
                  {t('moderation.reject')}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="space-y-4">
          {projects.data?.results.map((p) => (
            <li key={p.id} className="card space-y-3 p-5 sm:p-6">
              <h2 className="text-lg font-semibold">{p.title}</h2>
              <p className="text-sm text-muted">
                {p.owner.full_name} · {formatRelative(p.created_at)}
              </p>
              <p className="font-medium">{p.summary}</p>
              <p className="whitespace-pre-line text-muted">{p.description}</p>
              <div className="flex flex-wrap gap-3">
                <Button size="sm" onClick={() => setDecision({ kind: 'project', id: p.id, approve: true, title: p.title })}>
                  {t('moderation.approve')}
                </Button>
                <Button size="sm" variant="secondary" onClick={() => setDecision({ kind: 'project', id: p.id, approve: false, title: p.title })}>
                  {t('moderation.reject')}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Dialog
        open={decision !== null}
        onClose={() => setDecision(null)}
        title={decision?.approve ? t('moderation.approveTitle', { title: decision?.title }) : t('moderation.rejectTitle', { title: decision?.title ?? '' })}
        footer={
          <>
            <Button variant="text" onClick={() => setDecision(null)}>
              {t('common.cancel')}
            </Button>
            <Button variant={decision?.approve ? 'primary' : 'danger'} loading={decide.isPending} onClick={confirm}>
              {decision?.approve ? t('moderation.approve') : t('moderation.reject')}
            </Button>
          </>
        }
      >
        {decision?.approve && decision.kind === 'story' ? (
          <p className="text-muted">{t('moderation.approveBody')}</p>
        ) : (
          <TextArea
            label={decision?.kind === 'story' ? t('moderation.reason') : t('moderation.note')}
            hint={t('moderation.reasonHint')}
            value={note}
            maxLength={1000}
            onChange={(e) => setNote(e.target.value)}
            error={noteError}
            required={decision?.kind === 'story'}
          />
        )}
      </Dialog>
    </>
  )
}
