import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { MailCheck } from 'lucide-react'
import { ApiError } from '@/api/client'
import { authApi } from '@/api/endpoints'
import { Button, ButtonLink } from '@/components/ui/Button'
import { useErrorMessage } from '@/components/ui/feedback'
import { PasswordField, TextField } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'
import { AuthCard } from './AuthCard'
import { EMAIL_RE, validatePassword } from './validation'

export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const errorMessage = useErrorMessage()
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string>()
  const [sent, setSent] = useState(false)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!EMAIL_RE.test(email.trim())) {
      setError(t('validation.email'))
      return
    }
    setError(undefined)
    setLoading(true)
    try {
      await authApi.requestPasswordReset(email.trim().toLowerCase())
      setSent(true) // same message whether or not the account exists (no enumeration)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (sent)
    return (
      <AuthCard title={t('auth.checkEmailTitle')}>
        <div className="flex flex-col items-center gap-4 text-center">
          <MailCheck size={48} className="text-accent" aria-hidden />
          <p className="text-muted">{t('auth.checkEmailBody', { email })}</p>
          <ButtonLink to="/login" variant="secondary">
            {t('auth.backToSignIn')}
          </ButtonLink>
        </div>
      </AuthCard>
    )

  return (
    <AuthCard
      title={t('auth.forgotTitle')}
      subtitle={t('auth.forgotSubtitle')}
      footer={
        <Link to="/login" className="font-semibold text-primary hover:underline">
          {t('auth.backToSignIn')}
        </Link>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <TextField label={t('auth.email')} type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={error} required />
        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {t('auth.sendResetLink')}
        </Button>
      </form>
    </AuthCard>
  )
}

export function ResetPasswordPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const errorMessage = useErrorMessage()
  const [params] = useSearchParams()
  const uid = params.get('uid') ?? ''
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<{ password?: string; confirm?: string; form?: string }>({})
  const [loading, setLoading] = useState(false)

  if (!uid || !token)
    return (
      <AuthCard title={t('auth.resetInvalidTitle')} subtitle={t('auth.resetInvalidBody')}>
        <ButtonLink to="/forgot-password">{t('auth.sendResetLink')}</ButtonLink>
      </AuthCard>
    )

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const pw = validatePassword(password)
    const next = { password: pw && t(pw), confirm: confirm !== password ? t('validation.passwordMatch') : undefined }
    setErrors(next)
    if (next.password || next.confirm) return
    setLoading(true)
    try {
      await authApi.confirmPasswordReset({ uid, token, new_password: password })
      toast.success(t('auth.passwordUpdated'))
      navigate('/login', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'validation_error' && err.field('new_password')) setErrors({ password: err.field('new_password') })
      else if (err instanceof ApiError && err.status === 400) setErrors({ form: t('auth.resetInvalidBody') })
      else setErrors({ form: errorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthCard title={t('auth.resetTitle')}>
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        {errors.form && (
          <div role="alert" className="rounded-md border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
            {errors.form}
          </div>
        )}
        <PasswordField label={t('auth.newPassword')} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} hint={t('auth.passwordHint')} required />
        <PasswordField label={t('auth.confirmPassword')} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} error={errors.confirm} required />
        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {t('auth.updatePassword')}
        </Button>
      </form>
    </AuthCard>
  )
}
