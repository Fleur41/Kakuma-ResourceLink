import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '@/api/client'
import { authApi } from '@/api/endpoints'
import { Button } from '@/components/ui/Button'
import { useErrorMessage } from '@/components/ui/feedback'
import { PasswordField, TextField } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'
import { AuthCard } from './AuthCard'
import { EMAIL_RE, validatePassword } from './validation'

/** Seconds before another code can be requested (matches PASSWORD_RESET_RESEND_SECONDS on the server). */
const RESEND_SECONDS = 60
const CODE_RE = /^\d{6}$/

type Step = 'email' | 'code' | 'password'

/**
 * Password reset by emailed 6-digit code: email → code → new password.
 * The server answers the same whether or not the account exists, so the flow never reveals who has an account.
 */
export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const errorMessage = useErrorMessage()
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<{ email?: string; code?: string; password?: string; confirm?: string; form?: string }>({})
  const [loading, setLoading] = useState(false)
  const [resendIn, setResendIn] = useState(0)

  useEffect(() => {
    if (resendIn <= 0) return
    const id = window.setTimeout(() => setResendIn((s) => s - 1), 1000)
    return () => window.clearTimeout(id)
  }, [resendIn])

  async function sendCode() {
    await authApi.requestPasswordReset(email.trim().toLowerCase())
    setResendIn(RESEND_SECONDS)
  }

  async function onEmail(e: FormEvent) {
    e.preventDefault()
    if (!EMAIL_RE.test(email.trim())) return setErrors({ email: t('validation.email') })
    setErrors({})
    setLoading(true)
    try {
      await sendCode()
      setStep('code')
    } catch (err) {
      setErrors({ email: errorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  async function onResend() {
    setErrors({})
    try {
      await sendCode()
      setCode('')
      toast.success(t('auth.codeResent'))
    } catch (err) {
      setErrors({ form: errorMessage(err) })
    }
  }

  async function onCode(e: FormEvent) {
    e.preventDefault()
    if (!CODE_RE.test(code)) return setErrors({ code: t('auth.codeRequired') })
    setErrors({})
    setLoading(true)
    try {
      await authApi.verifyPasswordReset({ email: email.trim().toLowerCase(), code })
      setStep('password')
    } catch (err) {
      setErrors(err instanceof ApiError && err.field('code') ? { code: err.field('code') } : { form: errorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  async function onPassword(e: FormEvent) {
    e.preventDefault()
    const pw = validatePassword(password)
    const next = { password: pw && t(pw), confirm: confirm !== password ? t('validation.passwordMatch') : undefined }
    setErrors(next)
    if (next.password || next.confirm) return
    setLoading(true)
    try {
      await authApi.confirmPasswordReset({ email: email.trim().toLowerCase(), code, new_password: password })
      toast.success(t('auth.passwordUpdated'))
      navigate('/login', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.field('new_password')) setErrors({ password: err.field('new_password') })
      else if (err instanceof ApiError && err.field('code')) {
        // The code expired (or was replaced) while the person was typing: back to the code step.
        setStep('code')
        setErrors({ code: err.field('code') })
      } else setErrors({ form: errorMessage(err) })
    } finally {
      setLoading(false)
    }
  }

  const backToSignIn = (
    <Link to="/login" className="font-semibold text-primary hover:underline">
      {t('auth.backToSignIn')}
    </Link>
  )
  const formError = errors.form && (
    <div role="alert" className="rounded-md border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
      {errors.form}
    </div>
  )

  if (step === 'code')
    return (
      <AuthCard title={t('auth.codeTitle')} subtitle={t('auth.codeSubtitle', { email: email.trim() })} footer={backToSignIn}>
        <form noValidate onSubmit={onCode} className="space-y-5">
          {formError}
          <TextField
            label={t('auth.codeLabel')}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            inputMode="numeric"
            autoComplete="one-time-code"
            autoFocus
            maxLength={6}
            placeholder="000000"
            className="text-center font-mono text-2xl tracking-[0.5em]"
            error={errors.code}
            hint={t('auth.checkSpam')}
            required
          />
          <Button type="submit" size="lg" className="w-full" loading={loading}>
            {t('auth.verifyCode')}
          </Button>
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <button
              type="button"
              onClick={() => {
                setStep('email')
                setCode('')
                setErrors({})
              }}
              className="font-semibold text-primary hover:underline"
            >
              {t('auth.changeEmail')}
            </button>
            {resendIn > 0 ? (
              <span className="text-muted" aria-live="polite">
                {t('auth.resendIn', { seconds: resendIn })}
              </span>
            ) : (
              <button type="button" onClick={onResend} className="font-semibold text-primary hover:underline">
                {t('auth.resendCode')}
              </button>
            )}
          </div>
        </form>
      </AuthCard>
    )

  if (step === 'password')
    return (
      <AuthCard title={t('auth.resetTitle')} footer={backToSignIn}>
        <form noValidate onSubmit={onPassword} className="space-y-5">
          {formError}
          <PasswordField label={t('auth.newPassword')} autoComplete="new-password" autoFocus value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} hint={t('auth.passwordHint')} required />
          <PasswordField label={t('auth.confirmPassword')} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} error={errors.confirm} required />
          <Button type="submit" size="lg" className="w-full" loading={loading}>
            {t('auth.updatePassword')}
          </Button>
        </form>
      </AuthCard>
    )

  return (
    <AuthCard title={t('auth.forgotTitle')} subtitle={t('auth.forgotSubtitle')} footer={backToSignIn}>
      <form noValidate onSubmit={onEmail} className="space-y-5">
        <TextField label={t('auth.email')} type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} required />
        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {t('auth.sendCode')}
        </Button>
      </form>
    </AuthCard>
  )
}
