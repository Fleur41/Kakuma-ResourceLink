import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { ArrowLeft, CheckCircle2, HandHeart, MapPin } from 'lucide-react'
import { ApiError } from '@/api/client'
import { projectsApi } from '@/api/endpoints'
import type { ContributionType, Project } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge, StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { ErrorState, PageLoader, useErrorMessage } from '@/components/ui/feedback'
import { SelectField, TextArea, TextField } from '@/components/ui/Field'
import { useAuth } from '@/features/auth/auth-context'
import { formatAmount, formatRelative } from '@/lib/format'
import { useDocumentTitle } from '@/lib/hooks'
import { useRequireMember } from '@/lib/useRequireMember'
import { PledgeProgress } from './ProjectsPage'

export default function ProjectDetailPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const { user } = useAuth()
  const requireMember = useRequireMember()
  const [pledgeOpen, setPledgeOpen] = useState(false)
  const [confirmed, setConfirmed] = useState(false)
  const query = useQuery({ queryKey: ['projects', 'detail', id], queryFn: () => projectsApi.get(id) })
  useDocumentTitle(query.data?.title)

  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const p = query.data
  const isOwner = user?.id === p.owner.id
  const types = p.accepted_contribution_types?.length ? p.accepted_contribution_types : (['volunteering'] as ContributionType[])

  return (
    <div className="space-y-6">
      <Link to="/projects" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary hover:underline">
        <ArrowLeft size={18} aria-hidden />
        {t('projects.back')}
      </Link>
      {confirmed && (
        <div role="status" className="card flex items-start gap-3 border-accent p-5">
          <CheckCircle2 className="shrink-0 text-accent" aria-hidden />
          <div>
            <p className="font-semibold">{t('projects.pledgeConfirmedTitle')}</p>
            <p className="text-muted">{t('projects.pledgeConfirmedBody')}</p>
          </div>
        </div>
      )}
      <header className="card overflow-hidden">
        {p.cover_image ? <img src={p.cover_image} alt="" className="aspect-[16/6] w-full object-cover" /> : <div aria-hidden className="hero-gradient h-24" />}
        <div className="space-y-4 p-6 sm:p-8">
          <div className="flex flex-wrap gap-2">
            <Badge tone="gold">{t(`categories.project.${p.category ?? 'other'}`)}</Badge>
            <StatusBadge status={p.status} />
          </div>
          <h1 className="text-[28px] font-bold">{p.title}</h1>
          <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-muted">
            <span className="flex items-center gap-2">
              <Avatar name={p.owner.full_name} src={p.owner.avatar} size={28} />
              {t('projects.ledBy', { name: p.owner.full_name })}
            </span>
            {p.location && (
              <span className="flex items-center gap-2">
                <MapPin size={18} aria-hidden /> {p.location}
              </span>
            )}
          </div>
          <PledgeProgress project={p} />
          {p.status === 'active' && !isOwner && (
            <Button size="lg" icon={<HandHeart size={18} aria-hidden />} onClick={() => requireMember(() => setPledgeOpen(true))}>
              {t('projects.pledge')}
            </Button>
          )}
          {p.status === 'pending_review' && <p className="text-sm text-muted">{t('projects.pendingHelp')}</p>}
        </div>
      </header>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <section className="card space-y-4 p-6 sm:p-8">
          <h2 className="text-xl font-semibold">{t('projects.about')}</h2>
          <p className="font-medium">{p.summary}</p>
          <p className="whitespace-pre-line">{p.description}</p>
        </section>
        <aside className="space-y-4">
          <div className="card space-y-3 p-6">
            <h2 className="text-lg font-semibold">{t('projects.waysToHelp')}</h2>
            <ul className="flex flex-wrap gap-2">
              {types.map((ty) => (
                <li key={ty}>
                  <Badge>{t(`projects.types.${ty}`)}</Badge>
                </li>
              ))}
            </ul>
            <p className="text-sm text-muted">{p.contribution_summary.note || t('projects.pledgeNotice')}</p>
          </div>
          {user && <Contributions project={p} isOwner={isOwner} />}
        </aside>
      </div>
      {pledgeOpen && (
        <PledgeDialog
          project={p}
          types={types}
          onClose={() => setPledgeOpen(false)}
          onDone={() => {
            setPledgeOpen(false)
            setConfirmed(true)
          }}
        />
      )}
    </div>
  )
}

function Contributions({ project, isOwner }: { project: Project; isOwner: boolean }) {
  const { t } = useTranslation()
  const query = useQuery({ queryKey: ['projects', 'contributions', project.id], queryFn: () => projectsApi.contributions(project.id) })
  const items = query.data?.results ?? []
  if (query.isPending || query.isError || (!isOwner && items.length === 0)) return null
  return (
    <div className="card space-y-3 p-6">
      {/* Owners see every pledge; other members only see their own (backend privacy rule). */}
      <h2 className="text-lg font-semibold">{isOwner ? t('projects.allPledges') : t('projects.yourPledges')}</h2>
      {items.length === 0 ? (
        <p className="text-muted">{t('projects.noPledges')}</p>
      ) : (
        <ul className="space-y-3">
          {items.map((c) => (
            <li key={c.id} className="space-y-1 border-b border-border pb-3 last:border-0 last:pb-0">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-medium">{c.contributor.full_name}</span>
                <StatusBadge status={c.status} />
              </div>
              <p className="text-sm">
                {t(`projects.types.${c.type}`)}
                {c.amount ? ` · ${formatAmount(c.amount, c.currency)}` : ''} · <span className="text-muted">{formatRelative(c.created_at)}</span>
              </p>
              <p className="text-sm text-muted">{c.description}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function PledgeDialog({ project, types, onClose, onDone }: { project: Project; types: ContributionType[]; onClose: () => void; onDone: () => void }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const errorMessage = useErrorMessage()
  const [type, setType] = useState<ContributionType>(types[0])
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [errors, setErrors] = useState<{ description?: string; amount?: string; form?: string }>({})
  const financial = type === 'financial_pledge'

  const mutation = useMutation({
    mutationFn: () => projectsApi.pledge(project.id, { type, description: description.trim(), amount: financial ? amount : null }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['projects'] })
      onDone()
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'validation_error') setErrors({ description: e.field('description'), amount: e.field('amount'), form: e.field('type') })
      else setErrors({ form: errorMessage(e) })
    },
  })

  function submit() {
    const next: typeof errors = {}
    if (description.trim().length < 3) next.description = t('validation.required')
    if (financial && !(Number(amount) > 0)) next.amount = t('validation.positiveAmount')
    setErrors(next)
    if (!Object.keys(next).length) mutation.mutate()
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t('projects.pledgeTitle', { title: project.title })}
      footer={
        <>
          <Button variant="text" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button loading={mutation.isPending} onClick={submit}>
            {t('projects.confirmPledge')}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {errors.form && (
          <p role="alert" className="rounded-md bg-error/10 px-4 py-3 text-sm text-error">
            {errors.form}
          </p>
        )}
        <SelectField label={t('projects.pledgeType')} value={type} onChange={(e) => setType(e.target.value as ContributionType)} options={types.map((ty) => ({ value: ty, label: t(`projects.types.${ty}`) }))} />
        {financial && (
          <TextField
            label={t('projects.amount', { currency: project.currency ?? 'USD' })}
            type="number"
            inputMode="decimal"
            min="1"
            step="1"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            error={errors.amount}
            hint={t('projects.notAPayment')}
            required
          />
        )}
        <TextArea label={t('projects.pledgeDescription')} placeholder={t('projects.pledgeDescriptionPlaceholder')} value={description} maxLength={1000} onChange={(e) => setDescription(e.target.value)} error={errors.description} required />
        <p className="rounded-md bg-gold-container p-3 text-sm text-gold-text">{t('projects.pledgeNotice')}</p>
      </div>
    </Dialog>
  )
}
