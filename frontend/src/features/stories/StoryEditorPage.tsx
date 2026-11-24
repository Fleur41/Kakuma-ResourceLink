import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useBlocker, useNavigate, useParams } from 'react-router'
import { Check, Circle, ImagePlus, X } from 'lucide-react'
import { ApiError } from '@/api/client'
import { storiesApi, type StoryInput } from '@/api/endpoints'
import type { Language, StoryCategory, StoryDetail } from '@/api/types'
import { StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { ErrorState, PageLoader, useErrorMessage } from '@/components/ui/feedback'
import { Checkbox, SelectField, TextArea, TextField } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { currentLanguage, LANGUAGES } from '@/i18n'
import { cn, wordCount } from '@/lib/format'
import { STORY_CATEGORIES, useStory } from './hooks'
import { Markdown } from './Markdown'
import { validateImage, validateStory, type StoryErrors } from './storyValidation'

const LOCAL_DRAFT_KEY = 'kk.story-draft.new'

interface FormState {
  title: string
  body: string
  excerpt: string
  category: StoryCategory
  language: Language
  consent: boolean
}

function emptyForm(): FormState {
  return { title: '', body: '', excerpt: '', category: 'inspiration', language: currentLanguage(), consent: false }
}

function fromStory(s: StoryDetail): FormState {
  return {
    title: s.title,
    body: s.body,
    excerpt: s.excerpt ?? '',
    category: s.category ?? 'inspiration',
    language: s.language ?? 'en',
    consent: Boolean(s.consent_to_publish),
  }
}

function readLocalDraft(): FormState | null {
  try {
    const raw = localStorage.getItem(LOCAL_DRAFT_KEY)
    return raw ? { ...emptyForm(), ...(JSON.parse(raw) as Partial<FormState>) } : null
  } catch {
    return null
  }
}

export default function StoryEditorPage() {
  const { id } = useParams()
  const story = useStory(id ?? '', Boolean(id))
  const { user } = useAuth()
  const { t } = useTranslation()

  if (id && story.isPending) return <PageLoader />
  if (id && story.isError) return <ErrorState error={story.error} onRetry={() => void story.refetch()} />
  if (story.data && user && story.data.author.id !== user.id) return <ErrorState error={new ApiError(403, 'permission_denied', t('errors.forbidden'))} />
  // key resets the form when switching from "new" to the saved draft's id.
  return <Editor key={id ?? 'new'} existing={story.data} />
}

function Editor({ existing }: { existing?: StoryDetail }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const errorMessage = useErrorMessage()
  const { isModerator } = useAuth()

  const initial = useMemo(() => (existing ? fromStory(existing) : (readLocalDraft() ?? emptyForm())), [existing])
  const [form, setForm] = useState<FormState>(initial)
  const [cover, setCover] = useState<File | null>(null)
  const [coverError, setCoverError] = useState<string>()
  const [errors, setErrors] = useState<StoryErrors>({})
  const [tab, setTab] = useState<'write' | 'preview'>('write')
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)
  const skipBlock = useRef(false)
  const [restored, setRestored] = useState(() => !existing && readLocalDraft() !== null)

  const dirty = cover !== null || JSON.stringify(form) !== JSON.stringify(initial)
  const coverPreview = useMemo(() => (cover ? URL.createObjectURL(cover) : (existing?.cover_image ?? null)), [cover, existing])
  useEffect(() => () => void (cover && coverPreview && URL.revokeObjectURL(coverPreview)), [cover, coverPreview])

  // New stories autosave locally so nothing is lost on a crash, reload or lost connection.
  useEffect(() => {
    if (existing) return
    const id = setTimeout(() => {
      try {
        if (form.title || form.body) localStorage.setItem(LOCAL_DRAFT_KEY, JSON.stringify(form))
      } catch {
        /* ignore */
      }
    }, 600)
    return () => clearTimeout(id)
  }, [form, existing])

  const set = <K extends keyof FormState>(k: K, v: FormState[K]) => setForm((f) => ({ ...f, [k]: v }))

  const save = useMutation({
    mutationFn: async ({ submit }: { submit: boolean }) => {
      const input: StoryInput = {
        title: form.title.trim(),
        body: form.body,
        excerpt: form.excerpt.trim() || undefined,
        category: form.category,
        language: form.language,
        consent_to_publish: form.consent,
        cover,
      }
      let saved = existing
      if (!saved) saved = await storiesApi.create(input)
      else if (dirty || (submit && form.consent !== existing?.consent_to_publish)) saved = await storiesApi.update(saved.id, input)
      if (submit) saved = await storiesApi.publish(saved.id)
      return saved
    },
    onSuccess: (saved, { submit }) => {
      try {
        localStorage.removeItem(LOCAL_DRAFT_KEY)
      } catch {
        /* ignore */
      }
      qc.setQueryData(['stories', 'detail', saved.id], saved)
      void qc.invalidateQueries({ queryKey: ['stories', 'list'] })
      skipBlock.current = true
      if (submit) {
        toast.success(saved.status === 'published' ? t('stories.editor.published') : t('stories.editor.submitted'))
        navigate(`/stories/${saved.id}`, { replace: true })
      } else {
        toast.success(t('stories.editor.draftSaved'))
        setCover(null)
        navigate(`/stories/${saved.id}/edit`, { replace: true })
      }
    },
    onError: (err) => {
      if (err instanceof ApiError && err.code === 'validation_error') {
        setErrors({ title: err.field('title'), body: err.field('body'), consent: err.field('consent_to_publish') })
        if (err.field('cover_image')) setCoverError(err.field('cover_image'))
      }
      toast.error(errorMessage(err))
    },
  })

  // Warn before leaving with unsaved changes (in-app navigation and tab close).
  const blocker = useBlocker(({ currentLocation, nextLocation }) => !skipBlock.current && dirty && !save.isPending && currentLocation.pathname !== nextLocation.pathname)
  useEffect(() => {
    if (!dirty) return
    const handler = (e: BeforeUnloadEvent) => e.preventDefault()
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [dirty])

  function run(submit: boolean) {
    const v = validateStory({ title: form.title, body: form.body, consent: form.consent }, submit)
    setErrors(Object.fromEntries(Object.entries(v).map(([k, key]) => [k, t(key as string)])))
    if (Object.keys(v).length) {
      document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
      return
    }
    if (submit) setConfirmSubmit(true)
    else save.mutate({ submit: false })
  }

  function onPickCover(file?: File) {
    if (!file) return
    const err = validateImage(file)
    setCoverError(err && t(err))
    if (!err) setCover(file)
  }

  const words = wordCount(form.body)
  const checklist = [
    { done: form.title.trim().length >= 3, label: t('stories.editor.checkTitle') },
    { done: Boolean(coverPreview), label: t('stories.editor.checkCover') },
    { done: words >= 50, label: t('stories.editor.checkLength') },
    { done: form.consent, label: t('stories.editor.checkConsent') },
  ]

  return (
    <>
      <PageHeader
        title={existing ? t('stories.editor.editTitle') : t('stories.editor.title')}
        subtitle={t('stories.editor.subtitle')}
        actions={existing && <StatusBadge status={existing.status} />}
      />
      {restored && (
        <div className="card mb-6 flex flex-wrap items-center justify-between gap-3 p-4" role="status">
          <p className="text-sm">{t('stories.editor.restored')}</p>
          <Button
            variant="text"
            size="sm"
            onClick={() => {
              try {
                localStorage.removeItem(LOCAL_DRAFT_KEY)
              } catch {
                /* ignore */
              }
              setForm(emptyForm())
              setRestored(false)
            }}
          >
            {t('stories.editor.discardRestored')}
          </Button>
        </div>
      )}
      {existing?.status === 'published' && <p className="card mb-6 border-gold p-4 text-sm">{t('stories.editor.publishedWarning')}</p>}
      {existing?.status === 'rejected' && existing.rejection_reason && (
        <p className="card mb-6 border-error/40 p-4 text-sm">
          <span className="font-semibold">{t('stories.rejectionReason')}:</span> {existing.rejection_reason}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="card space-y-5 p-5 sm:p-8">
          <TextField label={t('stories.editor.storyTitle')} value={form.title} maxLength={200} onChange={(e) => set('title', e.target.value)} error={errors.title} required />

          <div>
            <p className="mb-1.5 text-sm font-medium">{t('stories.editor.cover')}</p>
            {coverPreview ? (
              <div className="relative overflow-hidden rounded-lg">
                <img src={coverPreview} alt={t('stories.editor.coverPreview')} className="aspect-[16/7] w-full object-cover" />
                <div className="absolute end-3 top-3 flex gap-2">
                  <Button size="sm" variant="inverse" onClick={() => fileInput.current?.click()}>
                    {t('stories.editor.changeCover')}
                  </Button>
                  {cover && (
                    <Button size="sm" variant="inverse" onClick={() => setCover(null)} aria-label={t('stories.editor.removeCover')}>
                      <X size={16} aria-hidden />
                    </Button>
                  )}
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                className="flex min-h-28 w-full flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-border text-muted hover:border-primary hover:text-primary"
              >
                <ImagePlus aria-hidden />
                <span className="font-medium">{t('stories.editor.addCover')}</span>
                <span className="text-xs">{t('stories.editor.coverHint')}</span>
              </button>
            )}
            <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" tabIndex={-1} aria-hidden onChange={(e) => onPickCover(e.target.files?.[0])} />
            {coverError && <p className="mt-1 text-sm text-error">{coverError}</p>}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField label={t('stories.editor.category')} value={form.category} onChange={(e) => set('category', e.target.value as StoryCategory)} options={STORY_CATEGORIES.map((c) => ({ value: c, label: t(`categories.story.${c}`) }))} />
            <SelectField label={t('stories.editor.language')} value={form.language} onChange={(e) => set('language', e.target.value as Language)} options={LANGUAGES.map((l) => ({ value: l.code, label: l.label }))} />
          </div>

          <div>
            <div role="tablist" aria-label={t('stories.editor.bodyMode')} className="mb-2 flex gap-1">
              {(['write', 'preview'] as const).map((m) => (
                <button
                  key={m}
                  type="button"
                  role="tab"
                  aria-selected={tab === m}
                  onClick={() => setTab(m)}
                  className={cn('min-h-10 rounded-md px-4 text-sm font-semibold', tab === m ? 'bg-primary-container text-primary' : 'text-muted hover:text-primary')}
                >
                  {t(`stories.editor.${m}`)}
                </button>
              ))}
            </div>
            {tab === 'write' ? (
              <TextArea
                label={t('stories.editor.body')}
                placeholder={t('stories.editor.bodyPlaceholder')}
                value={form.body}
                onChange={(e) => set('body', e.target.value)}
                error={errors.body}
                hint={t('stories.editor.markdownHint')}
                rows={16}
                required
              />
            ) : (
              <div className="min-h-64 rounded-md border border-border p-5">{form.body.trim() ? <Markdown>{form.body}</Markdown> : <p className="text-muted">{t('stories.editor.nothingToPreview')}</p>}</div>
            )}
            <p className="mt-1 text-end text-sm text-muted" aria-live="polite">
              {t('stories.editor.wordCount', { count: words })}
            </p>
          </div>

          <TextField label={t('stories.editor.excerpt')} value={form.excerpt} maxLength={300} onChange={(e) => set('excerpt', e.target.value)} hint={t('stories.editor.excerptHint')} />

          <Checkbox
            label={t('stories.editor.consent')}
            description={t('stories.editor.consentHelp')}
            checked={form.consent}
            onChange={(e) => set('consent', e.target.checked)}
            error={errors.consent}
          />
        </div>

        <aside className="space-y-4 lg:sticky lg:top-6 lg:h-fit">
          <div className="card space-y-4 p-6">
            <h2 className="text-lg font-semibold">{t('stories.editor.checklist')}</h2>
            <ul className="space-y-3">
              {checklist.map((c) => (
                <li key={c.label} className="flex items-start gap-3">
                  {c.done ? <Check size={20} className="shrink-0 text-accent" aria-hidden /> : <Circle size={20} className="shrink-0 text-border" aria-hidden />}
                  <span className={c.done ? '' : 'text-muted'}>
                    {c.label}
                    <span className="sr-only">{c.done ? ` (${t('common.done')})` : ` (${t('common.notDone')})`}</span>
                  </span>
                </li>
              ))}
            </ul>
            <p className="text-sm text-muted">{t('stories.editor.privacyTip')}</p>
          </div>
          <div className="flex flex-col gap-3">
            <Button variant="secondary" size="lg" loading={save.isPending && !save.variables?.submit} disabled={save.isPending} onClick={() => run(false)}>
              {t('stories.editor.saveDraft')}
            </Button>
            {existing?.status !== 'pending_review' && (
              <Button size="lg" loading={save.isPending && save.variables?.submit} disabled={save.isPending} onClick={() => run(true)}>
                {isModerator ? t('stories.editor.publish') : t('stories.editor.submit')}
              </Button>
            )}
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={confirmSubmit}
        title={t('stories.editor.submitConfirmTitle')}
        body={t('stories.editor.submitConfirmBody')}
        confirmLabel={isModerator ? t('stories.editor.publish') : t('stories.editor.submit')}
        loading={save.isPending}
        onConfirm={() => {
          setConfirmSubmit(false)
          save.mutate({ submit: true })
        }}
        onClose={() => setConfirmSubmit(false)}
      />
      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title={t('stories.editor.leaveTitle')}
        body={t('stories.editor.leaveBody')}
        confirmLabel={t('stories.editor.leave')}
        destructive
        onConfirm={() => blocker.proceed?.()}
        onClose={() => blocker.reset?.()}
      />
    </>
  )
}
