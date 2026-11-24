import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { UsersRound } from 'lucide-react'
import { mentorshipApi } from '@/api/endpoints'
import type { MentorshipRequest, MentorshipUpdateStatus } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge, StatusBadge } from '@/components/ui/Badge'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { CardSkeletons, EmptyState, ErrorState, useErrorMessage } from '@/components/ui/feedback'
import { TextArea } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { cn, formatRelative } from '@/lib/format'

type Box = 'received' | 'sent'

export default function MentorshipPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const box: Box = params.get('box') === 'sent' ? 'sent' : 'received'
  const query = useQuery({ queryKey: ['mentorship', box], queryFn: () => mentorshipApi.list({ box, page_size: 50 }) })
  const items = query.data?.results ?? []

  return (
    <>
      <PageHeader
        title={t('mentorship.title')}
        subtitle={t('mentorship.subtitle')}
        actions={
          <ButtonLink to="/alumni?mentors=1" variant="secondary">
            {t('mentorship.findMentor')}
          </ButtonLink>
        }
      />
      <div role="tablist" className="mb-6 flex gap-2 border-b border-border">
        {(['received', 'sent'] as const).map((k) => (
          <button
            key={k}
            role="tab"
            type="button"
            aria-selected={box === k}
            onClick={() => setParams(k === 'received' ? {} : { box: k }, { replace: true })}
            className={cn('-mb-px min-h-11 border-b-2 px-4 font-semibold', box === k ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-primary')}
          >
            {t(`mentorship.tabs.${k}`)}
          </button>
        ))}
      </div>
      {query.isPending ? (
        <CardSkeletons count={3} />
      ) : query.isError && !items.length ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<UsersRound aria-hidden />}
          title={box === 'sent' ? t('mentorship.noSent') : t('mentorship.noReceived')}
          body={box === 'sent' ? t('mentorship.noSentBody') : t('mentorship.noReceivedBody')}
          action={box === 'sent' && <ButtonLink to="/alumni?mentors=1">{t('mentorship.findMentor')}</ButtonLink>}
        />
      ) : (
        <ul className="space-y-4">
          {items.map((r) => (
            <RequestCard key={r.id} request={r} box={box} />
          ))}
        </ul>
      )}
    </>
  )
}

function RequestCard({ request: r, box }: { request: MentorshipRequest; box: Box }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [respond, setRespond] = useState<MentorshipUpdateStatus | null>(null)
  const [note, setNote] = useState('')
  const other = box === 'received' ? r.mentee : r.mentor

  const update = useMutation({
    mutationFn: (status: MentorshipUpdateStatus) => mentorshipApi.update(r.id, { status, response_message: note.trim() || undefined }),
    onSuccess: (_d, status) => {
      toast.success(t(`mentorship.updated.${status}`))
      setRespond(null)
      setNote('')
      void qc.invalidateQueries({ queryKey: ['mentorship'] })
      void qc.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const actions: { status: MentorshipUpdateStatus; variant: 'primary' | 'secondary' | 'text' }[] = []
  if (box === 'received' && r.status === 'pending') actions.push({ status: 'accepted', variant: 'primary' }, { status: 'declined', variant: 'secondary' })
  if (box === 'received' && r.status === 'accepted') actions.push({ status: 'completed', variant: 'secondary' })
  if (box === 'sent' && r.status === 'pending') actions.push({ status: 'cancelled', variant: 'text' })

  return (
    <li className="card space-y-4 p-5 sm:p-6">
      <div className="flex flex-wrap items-start gap-4">
        <Avatar name={other.full_name} src={other.avatar} size={48} />
        <div className="min-w-0 flex-1">
          <p className="text-sm text-muted">{box === 'received' ? t('mentorship.from') : t('mentorship.to')}</p>
          <h2 className="text-lg font-semibold">{other.full_name}</h2>
          <p className="text-sm text-muted">{formatRelative(r.created_at)}</p>
        </div>
        <StatusBadge status={r.status} />
      </div>
      <div className="space-y-2">
        <p>
          <span className="font-semibold">{t('mentorship.goal')}:</span> {r.goal}
        </p>
        <ul className="flex flex-wrap gap-2" aria-label={t('mentorship.helpAreas')}>
          {r.help_areas.map((a) => (
            <li key={a}>
              <Badge>{t(`mentorship.areas.${a}`)}</Badge>
            </li>
          ))}
        </ul>
        <p className="whitespace-pre-line text-muted">{r.message}</p>
        {r.response_message && (
          <p className="rounded-md bg-primary-container p-3 text-sm">
            <span className="font-semibold">{t('mentorship.response')}:</span> {r.response_message}
          </p>
        )}
      </div>
      {actions.length > 0 && (
        <div className="flex flex-wrap gap-3">
          {actions.map((a) => (
            <Button
              key={a.status}
              variant={a.variant}
              size="sm"
              loading={update.isPending && update.variables === a.status}
              disabled={update.isPending}
              onClick={() => (a.status === 'cancelled' || a.status === 'completed' ? update.mutate(a.status) : setRespond(a.status))}
            >
              {t(`mentorship.actions.${a.status}`)}
            </Button>
          ))}
        </div>
      )}
      <Dialog
        open={respond !== null}
        onClose={() => setRespond(null)}
        title={respond === 'accepted' ? t('mentorship.acceptTitle') : t('mentorship.declineTitle')}
        footer={
          <>
            <Button variant="text" onClick={() => setRespond(null)}>
              {t('common.cancel')}
            </Button>
            <Button variant={respond === 'declined' ? 'danger' : 'primary'} loading={update.isPending} onClick={() => respond && update.mutate(respond)}>
              {respond ? t(`mentorship.actions.${respond}`) : ''}
            </Button>
          </>
        }
      >
        <TextArea label={t('mentorship.responseOptional')} value={note} maxLength={1000} onChange={(e) => setNote(e.target.value)} />
      </Dialog>
    </li>
  )
}
