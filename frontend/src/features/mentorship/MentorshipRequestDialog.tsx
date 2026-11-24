import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { ApiError } from '@/api/client'
import { mentorshipApi } from '@/api/endpoints'
import type { HelpArea } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { useErrorMessage } from '@/components/ui/feedback'
import { TextArea, TextField } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'
import { cn } from '@/lib/format'

export const HELP_AREAS: HelpArea[] = ['career', 'technical', 'scholarships', 'entrepreneurship', 'other']

interface Props {
  open: boolean
  onClose: () => void
  mentorProfileId: string
  mentorName: string
}

export function MentorshipRequestDialog({ open, onClose, mentorProfileId, mentorName }: Props) {
  const { t } = useTranslation()
  const toast = useToast()
  const qc = useQueryClient()
  const errorMessage = useErrorMessage()
  const [goal, setGoal] = useState('')
  const [areas, setAreas] = useState<HelpArea[]>([])
  const [message, setMessage] = useState('')
  const [errors, setErrors] = useState<{ goal?: string; areas?: string; message?: string; form?: string }>({})

  const mutation = useMutation({
    mutationFn: () => mentorshipApi.create({ mentor_id: mentorProfileId, goal: goal.trim(), help_areas: areas, message: message.trim() }),
    onSuccess: () => {
      toast.success(t('mentorship.requestSent'))
      void qc.invalidateQueries({ queryKey: ['mentorship'] })
      setGoal('')
      setAreas([])
      setMessage('')
      onClose()
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'validation_error')
        setErrors({ goal: e.field('goal'), areas: e.field('help_areas'), message: e.field('message'), form: e.field('mentor_id') ?? (e.field('goal') || e.field('message') ? undefined : e.message) })
      else setErrors({ form: errorMessage(e) })
    },
  })

  function submit() {
    const next: typeof errors = {}
    if (goal.trim().length < 3) next.goal = t('validation.required')
    if (!areas.length) next.areas = t('mentorship.pickArea')
    if (message.trim().length < 10) next.message = t('validation.minLength', { count: 10 })
    setErrors(next)
    if (Object.keys(next).length === 0) mutation.mutate()
  }

  const toggle = (a: HelpArea) => setAreas((list) => (list.includes(a) ? list.filter((x) => x !== a) : [...list, a]))

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('mentorship.requestTitle', { name: mentorName })}
      footer={
        <>
          <Button variant="text" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button loading={mutation.isPending} onClick={submit}>
            {t('mentorship.sendRequest')}
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
        <TextField label={t('mentorship.goal')} placeholder={t('mentorship.goalPlaceholder')} value={goal} maxLength={200} onChange={(e) => setGoal(e.target.value)} error={errors.goal} required />
        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('mentorship.helpAreas')}</legend>
          <div className="flex flex-wrap gap-2">
            {HELP_AREAS.map((a) => (
              <label key={a} className={cn('flex min-h-10 cursor-pointer items-center gap-2 rounded-full border px-4 text-sm font-medium', areas.includes(a) ? 'border-primary bg-primary-container text-primary' : 'border-border')}>
                <input type="checkbox" className="sr-only" checked={areas.includes(a)} onChange={() => toggle(a)} />
                {t(`mentorship.areas.${a}`)}
              </label>
            ))}
          </div>
          {errors.areas && <p className="mt-1 text-sm text-error">{errors.areas}</p>}
        </fieldset>
        <TextArea label={t('mentorship.message')} placeholder={t('mentorship.messagePlaceholder')} value={message} maxLength={2000} onChange={(e) => setMessage(e.target.value)} error={errors.message} required />
        <p className="text-sm text-muted">{t('mentorship.privacyNote')}</p>
      </div>
    </Dialog>
  )
}
