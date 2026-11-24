import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '@/api/client'
import { projectsApi } from '@/api/endpoints'
import type { ContributionType, ProjectCategory } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { useErrorMessage } from '@/components/ui/feedback'
import { SelectField, TextArea, TextField } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { cn } from '@/lib/format'
import { PROJECT_CATEGORIES } from './ProjectsPage'

const TYPES: ContributionType[] = ['mentoring', 'volunteering', 'equipment', 'skills', 'financial_pledge']

export default function NewProjectPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const errorMessage = useErrorMessage()
  const [f, setF] = useState({ title: '', summary: '', description: '', category: 'education' as ProjectCategory, location: '', funding_goal: '', currency: 'USD' })
  const [types, setTypes] = useState<ContributionType[]>(['volunteering'])
  const [errors, setErrors] = useState<Record<string, string | undefined>>({})
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => setF((s) => ({ ...s, [k]: e.target.value }))

  const mutation = useMutation({
    mutationFn: () =>
      projectsApi.create({
        title: f.title.trim(),
        summary: f.summary.trim(),
        description: f.description.trim(),
        category: f.category,
        location: f.location.trim(),
        accepted_contribution_types: types,
        funding_goal: types.includes('financial_pledge') && f.funding_goal ? f.funding_goal : null,
        currency: f.currency.trim().toUpperCase(),
      }),
    onSuccess: (p) => {
      void qc.invalidateQueries({ queryKey: ['projects'] })
      toast.success(p.status === 'active' ? t('projects.created') : t('projects.submitted'))
      navigate(`/projects/${p.id}`, { replace: true })
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'validation_error')
        setErrors({ title: e.field('title'), summary: e.field('summary'), description: e.field('description'), currency: e.field('currency'), funding_goal: e.field('funding_goal') })
      toast.error(errorMessage(e))
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const next: Record<string, string | undefined> = {}
    if (f.title.trim().length < 3) next.title = t('validation.titleLength')
    if (!f.summary.trim()) next.summary = t('validation.required')
    if (!f.description.trim()) next.description = t('validation.required')
    if (!types.length) next.types = t('projects.pickType')
    if (!/^[A-Za-z]{3}$/.test(f.currency.trim())) next.currency = t('validation.currency')
    setErrors(next)
    if (!Object.values(next).some(Boolean)) mutation.mutate()
  }

  const toggle = (ty: ContributionType) => setTypes((l) => (l.includes(ty) ? l.filter((x) => x !== ty) : [...l, ty]))

  return (
    <>
      <PageHeader title={t('projects.newTitle')} subtitle={t('projects.newSubtitle')} />
      <form noValidate onSubmit={onSubmit} className="card max-w-[760px] space-y-5 p-5 sm:p-8">
        <TextField label={t('projects.fields.title')} value={f.title} maxLength={200} onChange={set('title')} error={errors.title} required />
        <TextField label={t('projects.fields.summary')} value={f.summary} maxLength={300} onChange={set('summary')} error={errors.summary} required />
        <TextArea label={t('projects.fields.description')} value={f.description} onChange={set('description')} error={errors.description} rows={8} required />
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField label={t('projects.fields.category')} value={f.category} onChange={set('category')} options={PROJECT_CATEGORIES.map((c) => ({ value: c, label: t(`categories.project.${c}`) }))} />
          <TextField label={t('projects.fields.location')} value={f.location} onChange={set('location')} />
        </div>
        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('projects.waysToHelp')}</legend>
          <div className="flex flex-wrap gap-2">
            {TYPES.map((ty) => (
              <label key={ty} className={cn('flex min-h-10 cursor-pointer items-center gap-2 rounded-full border px-4 text-sm font-medium', types.includes(ty) ? 'border-primary bg-primary-container text-primary' : 'border-border')}>
                <input type="checkbox" className="sr-only" checked={types.includes(ty)} onChange={() => toggle(ty)} />
                {t(`projects.types.${ty}`)}
              </label>
            ))}
          </div>
          {errors.types && <p className="mt-1 text-sm text-error">{errors.types}</p>}
        </fieldset>
        {types.includes('financial_pledge') && (
          <div className="grid gap-4 sm:grid-cols-[1fr_140px]">
            <TextField label={t('projects.fields.goal')} type="number" min="1" inputMode="decimal" value={f.funding_goal} onChange={set('funding_goal')} error={errors.funding_goal} hint={t('projects.notAPayment')} />
            <TextField label={t('projects.fields.currency')} value={f.currency} maxLength={3} onChange={set('currency')} error={errors.currency} />
          </div>
        )}
        <p className="text-sm text-muted">{t('projects.reviewNote')}</p>
        <Button type="submit" size="lg" loading={mutation.isPending}>
          {t('projects.submitProposal')}
        </Button>
      </form>
    </>
  )
}
