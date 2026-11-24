import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '@/api/client'
import { alumniApi, authApi } from '@/api/endpoints'
import type { AlumniOwn, AlumniVisibility } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { ErrorState, PageLoader, useErrorMessage } from '@/components/ui/feedback'
import { Checkbox, TextArea, TextField } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { cn, parseTags } from '@/lib/format'

const VISIBILITY: AlumniVisibility[] = ['public', 'members', 'private']

export default function AlumniEditorPage() {
  const query = useQuery({
    queryKey: ['alumni', 'mine'],
    queryFn: async () => {
      try {
        return await alumniApi.mine()
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) return null // not in the directory yet
        throw e
      }
    },
  })
  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  return <AlumniForm existing={query.data} />
}

function AlumniForm({ existing }: { existing: AlumniOwn | null }) {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { setUser } = useAuth()
  const errorMessage = useErrorMessage()

  const [f, setF] = useState({
    country: existing?.country ?? '',
    city: existing?.city ?? '',
    profession: existing?.profession ?? '',
    organization: existing?.organization ?? '',
    bio: existing?.bio ?? '',
    skills: (existing?.skills ?? []).join(', '),
    is_mentor: existing?.is_mentor ?? false,
    mentorship_areas: (existing?.mentorship_areas ?? []).join(', '),
    willing_to_contribute: existing?.willing_to_contribute ?? false,
    contribution_interests: (existing?.contribution_interests ?? []).join(', '),
    visibility: existing?.visibility ?? ('members' as AlumniVisibility),
    show_contact_email: existing?.show_contact_email ?? false,
    show_city: existing?.show_city ?? true,
    consent: Boolean(existing),
  })
  const [errors, setErrors] = useState<Record<string, string | undefined>>({})
  const [withdrawOpen, setWithdrawOpen] = useState(false)
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((s) => ({ ...s, [k]: v }))

  const afterChange = async () => {
    void qc.invalidateQueries({ queryKey: ['alumni'] })
    try {
      setUser(await authApi.me()) // has_alumni_profile / role may change
    } catch {
      /* ignore */
    }
  }

  const save = useMutation({
    mutationFn: () =>
      alumniApi.save(
        {
          country: f.country.trim(),
          city: f.city.trim(),
          profession: f.profession.trim(),
          organization: f.organization.trim(),
          bio: f.bio.trim(),
          skills: parseTags(f.skills),
          is_mentor: f.is_mentor,
          mentorship_areas: f.is_mentor ? parseTags(f.mentorship_areas) : [],
          willing_to_contribute: f.willing_to_contribute,
          contribution_interests: f.willing_to_contribute ? parseTags(f.contribution_interests) : [],
          visibility: f.visibility,
          show_contact_email: f.show_contact_email,
          show_city: f.show_city,
          consent: f.consent,
        },
        Boolean(existing),
      ),
    onSuccess: async (saved) => {
      toast.success(existing ? t('alumni.editor.saved') : t('alumni.editor.created'))
      await afterChange()
      navigate(`/alumni/${saved.id}`)
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'validation_error')
        setErrors({ country: e.field('country'), profession: e.field('profession'), consent: e.field('consent'), bio: e.field('bio') })
      toast.error(errorMessage(e))
    },
  })

  const withdraw = useMutation({
    mutationFn: alumniApi.withdraw,
    onSuccess: async () => {
      setWithdrawOpen(false)
      toast.success(t('alumni.editor.withdrawn'))
      await afterChange()
      navigate('/alumni')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const next: Record<string, string | undefined> = {}
    if (!f.country.trim()) next.country = t('validation.required')
    if (!f.profession.trim()) next.profession = t('validation.required')
    if (!f.consent) next.consent = t('alumni.editor.consentRequired')
    setErrors(next)
    if (Object.values(next).some(Boolean)) return
    save.mutate()
  }

  return (
    <>
      <PageHeader title={existing ? t('alumni.editor.editTitle') : t('alumni.editor.createTitle')} subtitle={t('alumni.editor.subtitle')} />
      <form noValidate onSubmit={onSubmit} className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="card space-y-5 p-5 sm:p-8">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label={t('alumni.editor.profession')} value={f.profession} onChange={(e) => set('profession', e.target.value)} error={errors.profession} required />
            <TextField label={t('alumni.editor.organization')} value={f.organization} onChange={(e) => set('organization', e.target.value)} />
            <TextField label={t('alumni.editor.country')} value={f.country} onChange={(e) => set('country', e.target.value)} error={errors.country} autoComplete="country-name" required />
            <TextField label={t('alumni.editor.city')} value={f.city} onChange={(e) => set('city', e.target.value)} autoComplete="address-level2" />
          </div>
          <TextArea label={t('alumni.editor.bio')} value={f.bio} maxLength={2000} onChange={(e) => set('bio', e.target.value)} error={errors.bio} rows={5} />
          <TextField label={t('alumni.skills')} value={f.skills} onChange={(e) => set('skills', e.target.value)} hint={t('alumni.editor.tagsHint')} />
          <Checkbox label={t('alumni.editor.isMentor')} description={t('alumni.editor.isMentorHelp')} checked={f.is_mentor} onChange={(e) => set('is_mentor', e.target.checked)} />
          {f.is_mentor && <TextField label={t('alumni.mentorshipAreas')} value={f.mentorship_areas} onChange={(e) => set('mentorship_areas', e.target.value)} hint={t('alumni.editor.tagsHint')} />}
          <Checkbox label={t('alumni.editor.willing')} checked={f.willing_to_contribute} onChange={(e) => set('willing_to_contribute', e.target.checked)} />
          {f.willing_to_contribute && (
            <TextField label={t('alumni.contributionInterests')} value={f.contribution_interests} onChange={(e) => set('contribution_interests', e.target.value)} hint={t('alumni.editor.tagsHint')} />
          )}
        </div>

        <aside className="space-y-4">
          <div className="card space-y-4 p-6">
            <h2 className="text-lg font-semibold">{t('alumni.editor.privacy')}</h2>
            <fieldset className="space-y-2">
              <legend className="mb-1 text-sm font-medium">{t('alumni.editor.visibility')}</legend>
              {VISIBILITY.map((v) => (
                <label key={v} className={cn('flex cursor-pointer items-start gap-3 rounded-md border p-3', f.visibility === v ? 'border-primary bg-primary-container' : 'border-border')}>
                  <input type="radio" name="visibility" checked={f.visibility === v} onChange={() => set('visibility', v)} className="mt-1 accent-[var(--kk-primary)]" />
                  <span>
                    <span className="block font-medium">{t(`alumni.visibility.${v}`)}</span>
                    <span className="block text-sm text-muted">{t(`alumni.visibility.${v}Help`)}</span>
                  </span>
                </label>
              ))}
            </fieldset>
            <Checkbox label={t('alumni.editor.showEmail')} description={t('alumni.editor.showEmailHelp')} checked={f.show_contact_email} onChange={(e) => set('show_contact_email', e.target.checked)} />
            <Checkbox label={t('alumni.editor.showCity')} checked={f.show_city} onChange={(e) => set('show_city', e.target.checked)} />
          </div>
          <div className="card space-y-4 p-6">
            <Checkbox label={t('alumni.editor.consent')} description={t('alumni.editor.consentHelp')} checked={f.consent} onChange={(e) => set('consent', e.target.checked)} error={errors.consent} disabled={Boolean(existing)} />
            <Button type="submit" size="lg" className="w-full" loading={save.isPending}>
              {existing ? t('common.saveChanges') : t('alumni.editor.create')}
            </Button>
            {existing && (
              <Button variant="text" className="w-full text-error" onClick={() => setWithdrawOpen(true)}>
                {t('alumni.editor.withdraw')}
              </Button>
            )}
          </div>
        </aside>
      </form>
      <ConfirmDialog
        open={withdrawOpen}
        title={t('alumni.editor.withdrawTitle')}
        body={t('alumni.editor.withdrawBody')}
        confirmLabel={t('alumni.editor.withdraw')}
        destructive
        loading={withdraw.isPending}
        onConfirm={() => withdraw.mutate()}
        onClose={() => setWithdrawOpen(false)}
      />
    </>
  )
}
