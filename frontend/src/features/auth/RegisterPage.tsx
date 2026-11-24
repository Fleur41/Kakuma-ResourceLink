import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '@/api/client'
import type { Language, RegisterRole } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { useErrorMessage } from '@/components/ui/feedback'
import { Checkbox, PasswordField, SelectField, TextField } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'
import { currentLanguage, LANGUAGES } from '@/i18n'
import { cn } from '@/lib/format'
import { useAuth } from './auth-context'
import { AuthCard } from './AuthCard'
import { safeNext } from './guards'
import { validateRegister, type FieldErrors } from './validation'

type Field = 'full_name' | 'email' | 'password' | 'confirm' | 'terms'
const ROLES: RegisterRole[] = ['member', 'alumni', 'mentor']

export default function RegisterPage() {
  const { t } = useTranslation()
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const errorMessage = useErrorMessage()

  const [form, setForm] = useState({ full_name: '', email: '', password: '', confirm: '', terms: false })
  const [role, setRole] = useState<RegisterRole>('member')
  const [language, setLanguage] = useState<Language>(currentLanguage())
  const [errors, setErrors] = useState<FieldErrors<Field>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }))

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (loading) return
    const v = validateRegister(form)
    const translated = Object.fromEntries(Object.entries(v).map(([k, key]) => [k, t(key as string)])) as FieldErrors<Field>
    setErrors(translated)
    setFormError(null)
    if (Object.keys(v).length) return
    setLoading(true)
    try {
      await register({ full_name: form.full_name, email: form.email, password: form.password, role, preferred_language: language })
      toast.success(t('auth.accountCreated'))
      navigate(safeNext(params.get('next')), { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'validation_error') {
        setErrors({ full_name: err.field('full_name'), email: err.field('email'), password: err.field('password') })
        if (!err.field('email') && !err.field('password') && !err.field('full_name')) setFormError(err.message)
      } else {
        setFormError(errorMessage(err))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthCard
      title={t('auth.registerTitle')}
      subtitle={t('auth.registerSubtitle')}
      footer={
        <>
          {t('auth.haveAccount')}{' '}
          <Link to="/login" className="font-semibold text-primary hover:underline">
            {t('auth.signIn')}
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        {formError && (
          <div role="alert" className="rounded-md border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
            {formError}
          </div>
        )}
        <TextField label={t('auth.fullName')} autoComplete="name" value={form.full_name} onChange={set('full_name')} error={errors.full_name} required />
        <TextField label={t('auth.email')} type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} required />
        <PasswordField
          label={t('auth.password')}
          autoComplete="new-password"
          value={form.password}
          onChange={set('password')}
          error={errors.password}
          hint={t('auth.passwordHint')}
          required
        />
        <PasswordField label={t('auth.confirmPassword')} autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} required />

        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t('auth.iAm')}</legend>
          <div className="grid gap-2 sm:grid-cols-3">
            {ROLES.map((r) => (
              <label
                key={r}
                className={cn(
                  'flex min-h-12 cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium',
                  role === r ? 'border-primary bg-primary-container text-primary' : 'border-border',
                )}
              >
                <input type="radio" name="role" value={r} checked={role === r} onChange={() => setRole(r)} className="accent-[var(--kk-primary)]" />
                {t(`roles.${r}`)}
              </label>
            ))}
          </div>
        </fieldset>

        <SelectField
          label={t('auth.preferredLanguage')}
          value={language}
          onChange={(e) => setLanguage(e.target.value as Language)}
          options={LANGUAGES.map((l) => ({ value: l.code, label: l.label }))}
        />

        <Checkbox label={t('auth.terms')} checked={form.terms} onChange={set('terms')} error={errors.terms} />

        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {t('auth.createAccount')}
        </Button>
      </form>
    </AuthCard>
  )
}
