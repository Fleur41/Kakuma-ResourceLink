import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { useErrorMessage } from '@/components/ui/feedback'
import { PasswordField, TextField } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from './auth-context'
import { AuthCard } from './AuthCard'
import { safeNext } from './guards'
import { validateLogin, type FieldErrors } from './validation'

export default function LoginPage() {
  const { t } = useTranslation()
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const errorMessage = useErrorMessage()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors<'email' | 'password'>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (loading) return
    const v = validateLogin(email, password)
    setErrors(v)
    setFormError(null)
    if (Object.keys(v).length) return
    setLoading(true)
    try {
      const user = await login(email, password)
      toast.success(t('auth.welcomeBack', { name: user.full_name.split(' ')[0] }))
      navigate(safeNext(params.get('next')), { replace: true })
    } catch (err) {
      setFormError(err instanceof ApiError && err.status === 401 ? t('auth.invalidCredentials') : errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthCard
      title={t('auth.signInTitle')}
      subtitle={t('auth.signInSubtitle')}
      footer={
        <>
          {t('auth.noAccount')}{' '}
          <Link to={`/register${params.get('next') ? `?next=${encodeURIComponent(params.get('next') ?? '')}` : ''}`} className="font-semibold text-primary underline-offset-2 hover:underline">
            {t('auth.createAccount')}
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
        <TextField
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          inputMode="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email && t(errors.email)}
          required
        />
        <PasswordField
          label={t('auth.password')}
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password && t(errors.password)}
          required
        />
        <div className="flex justify-end">
          <Link to="/forgot-password" className="text-sm font-semibold text-primary hover:underline">
            {t('auth.forgotPassword')}
          </Link>
        </div>
        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {t('auth.signIn')}
        </Button>
        <Link to="/" className="block text-center text-sm font-semibold text-muted hover:text-primary">
          {t('auth.browseAsGuest')}
        </Link>
      </form>
    </AuthCard>
  )
}
