import { useRef, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { Camera, ChevronRight, LogOut } from 'lucide-react'
import { ApiError } from '@/api/client'
import { authApi } from '@/api/endpoints'
import type { Language } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { useErrorMessage } from '@/components/ui/feedback'
import { PasswordField, SelectField, TextArea, TextField } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { validatePassword } from '@/features/auth/validation'
import { validateImage } from '@/features/stories/storyValidation'
import { currentLanguage, LANGUAGES } from '@/i18n'
import { CACHE_KEY } from '@/app/queryClient'
import { formatDate } from '@/lib/format'
import { getThemePref, setThemePref, type ThemePref } from '@/lib/theme'

export default function ProfilePage() {
  const { t } = useTranslation()
  const { user, setUser, setLanguage, logout } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const errorMessage = useErrorMessage()
  const fileRef = useRef<HTMLInputElement>(null)
  const [theme, setTheme] = useState<ThemePref>(getThemePref())
  const [logoutOpen, setLogoutOpen] = useState(false)
  const [form, setForm] = useState({ full_name: user?.full_name ?? '', bio: user?.profile?.bio ?? '', location: user?.profile?.location ?? '' })
  const [errors, setErrors] = useState<{ full_name?: string }>({})

  const save = useMutation({
    mutationFn: () => authApi.updateMe({ full_name: form.full_name.trim(), profile: { bio: form.bio.trim(), location: form.location.trim() } }),
    onSuccess: (u) => {
      setUser(u)
      toast.success(t('profile.saved'))
    },
    onError: (e) => {
      if (e instanceof ApiError && e.field('full_name')) setErrors({ full_name: e.field('full_name') })
      toast.error(errorMessage(e))
    },
  })

  const avatar = useMutation({
    mutationFn: authApi.uploadAvatar,
    onSuccess: (u) => {
      setUser(u)
      toast.success(t('profile.photoUpdated'))
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (!user) return null

  function onSave(e: FormEvent) {
    e.preventDefault()
    if (form.full_name.trim().length < 2) return setErrors({ full_name: t('validation.name') })
    setErrors({})
    save.mutate()
  }

  function clearOffline() {
    try {
      localStorage.removeItem(CACHE_KEY)
    } catch {
      /* ignore */
    }
    toast.success(t('profile.offlineCleared'))
  }

  return (
    <>
      <PageHeader title={t('profile.title')} />
      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <section className="card p-5 sm:p-8" aria-labelledby="account-h">
            <div className="mb-6 flex flex-wrap items-center gap-5">
              <div className="relative">
                <Avatar name={user.full_name} src={user.profile?.avatar} size={88} />
                <button
                  type="button"
                  onClick={() => fileRef.current?.click()}
                  aria-label={t('profile.changePhoto')}
                  className="absolute -end-1 -bottom-1 flex size-9 items-center justify-center rounded-full bg-primary text-on-primary ring-4 ring-surface"
                  disabled={avatar.isPending}
                >
                  <Camera size={16} aria-hidden />
                </button>
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="sr-only"
                  tabIndex={-1}
                  aria-hidden
                  onChange={(e) => {
                    const file = e.target.files?.[0]
                    e.target.value = ''
                    if (!file) return
                    const err = validateImage(file)
                    if (err) toast.error(t(err))
                    else avatar.mutate(file)
                  }}
                />
              </div>
              <div className="min-w-0">
                <h2 id="account-h" className="text-xl font-semibold">
                  {user.full_name}
                </h2>
                <p className="text-muted">{user.email}</p>
                <p className="text-sm text-muted">
                  {t(`roles.${user.role}`)} · {t('profile.memberSince', { date: formatDate(user.date_joined) })}
                </p>
              </div>
            </div>
            <form noValidate onSubmit={onSave} className="space-y-5">
              <TextField label={t('auth.fullName')} value={form.full_name} onChange={(e) => setForm((f) => ({ ...f, full_name: e.target.value }))} error={errors.full_name} autoComplete="name" required />
              <TextField label={t('profile.location')} value={form.location} onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))} />
              <TextArea label={t('profile.bio')} value={form.bio} maxLength={1000} onChange={(e) => setForm((f) => ({ ...f, bio: e.target.value }))} />
              <Button type="submit" loading={save.isPending}>
                {t('common.saveChanges')}
              </Button>
            </form>
          </section>
          <ChangePassword />
        </div>

        <aside className="space-y-6">
          <section className="card space-y-5 p-6" aria-labelledby="prefs-h">
            <h2 id="prefs-h" className="text-lg font-semibold">
              {t('profile.preferences')}
            </h2>
            <SelectField
              label={t('common.language')}
              value={currentLanguage()}
              onChange={(e) => setLanguage(e.target.value as Language)}
              options={LANGUAGES.map((l) => ({ value: l.code, label: l.label }))}
              hint={t('profile.languageHint')}
            />
            <SelectField
              label={t('profile.theme')}
              value={theme}
              onChange={(e) => {
                const v = e.target.value as ThemePref
                setTheme(v)
                setThemePref(v)
              }}
              options={(['system', 'light', 'dark'] as const).map((v) => ({ value: v, label: t(`profile.themes.${v}`) }))}
            />
          </section>
          <section className="card divide-y divide-border overflow-hidden">
            {[
              { to: '/alumni/me', label: user.has_alumni_profile ? t('alumni.editMyProfile') : t('alumni.joinDirectory') },
              { to: '/my-stories', label: t('stories.myStories') },
              { to: '/opportunities?saved=1', label: t('profile.savedOpportunities') },
              { to: '/mentorship', label: t('nav.mentorship') },
            ].map((l) => (
              <Link key={l.to} to={l.to} className="flex min-h-14 items-center justify-between px-6 font-medium hover:bg-primary-container/50">
                {l.label}
                <ChevronRight size={18} aria-hidden className="text-muted" />
              </Link>
            ))}
          </section>
          <section className="card space-y-3 p-6">
            <h2 className="text-lg font-semibold">{t('profile.offline')}</h2>
            <p className="text-sm text-muted">{t('profile.offlineHelp')}</p>
            <Button variant="secondary" size="sm" onClick={clearOffline}>
              {t('profile.clearOffline')}
            </Button>
          </section>
          <Button variant="text" className="w-full text-error" icon={<LogOut size={18} aria-hidden />} onClick={() => setLogoutOpen(true)}>
            {t('auth.signOut')}
          </Button>
        </aside>
      </div>
      <ConfirmDialog
        open={logoutOpen}
        title={t('auth.signOutTitle')}
        body={t('auth.signOutBody')}
        confirmLabel={t('auth.signOut')}
        onConfirm={async () => {
          setLogoutOpen(false)
          await logout()
          toast.success(t('auth.signedOut'))
          navigate('/', { replace: true })
        }}
        onClose={() => setLogoutOpen(false)}
      />
    </>
  )
}

function ChangePassword() {
  const { t } = useTranslation()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [f, setF] = useState({ old: '', next: '', confirm: '' })
  const [errors, setErrors] = useState<{ old?: string; next?: string; confirm?: string }>({})
  const mutation = useMutation({
    mutationFn: () => authApi.changePassword({ old_password: f.old, new_password: f.next }),
    onSuccess: () => {
      setF({ old: '', next: '', confirm: '' })
      toast.success(t('auth.passwordUpdated'))
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'validation_error') setErrors({ old: e.field('old_password'), next: e.field('new_password') })
      else toast.error(errorMessage(e))
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const pw = validatePassword(f.next)
    const next = { old: f.old ? undefined : t('validation.required'), next: pw && t(pw), confirm: f.confirm !== f.next ? t('validation.passwordMatch') : undefined }
    setErrors(next)
    if (!next.old && !next.next && !next.confirm) mutation.mutate()
  }

  return (
    <section className="card p-5 sm:p-8" aria-labelledby="pw-h">
      <h2 id="pw-h" className="mb-5 text-lg font-semibold">
        {t('profile.changePassword')}
      </h2>
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <PasswordField label={t('profile.currentPassword')} autoComplete="current-password" value={f.old} onChange={(e) => setF((s) => ({ ...s, old: e.target.value }))} error={errors.old} required />
        <PasswordField label={t('auth.newPassword')} autoComplete="new-password" value={f.next} onChange={(e) => setF((s) => ({ ...s, next: e.target.value }))} error={errors.next} hint={t('auth.passwordHint')} required />
        <PasswordField label={t('auth.confirmPassword')} autoComplete="new-password" value={f.confirm} onChange={(e) => setF((s) => ({ ...s, confirm: e.target.value }))} error={errors.confirm} required />
        <Button type="submit" variant="secondary" loading={mutation.isPending}>
          {t('auth.updatePassword')}
        </Button>
      </form>
    </section>
  )
}
