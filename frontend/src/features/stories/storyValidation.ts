export const MAX_IMAGE_BYTES = 5 * 1024 * 1024
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export interface StoryDraft {
  title: string
  body: string
  consent: boolean
}

export type StoryErrors = Partial<Record<'title' | 'body' | 'consent', string>>

/** Returns i18n keys. Drafts need a title and some text; submitting also needs consent (backend rule). */
export function validateStory(d: StoryDraft, submitting: boolean): StoryErrors {
  const e: StoryErrors = {}
  const title = d.title.trim()
  if (title.length < 3) e.title = 'validation.titleLength'
  else if (title.length > 200) e.title = 'validation.titleMax'
  if (!d.body.trim()) e.body = 'validation.required'
  else if (d.body.length > 50_000) e.body = 'validation.bodyMax'
  if (submitting && !d.consent) e.consent = 'stories.editor.consentRequired'
  return e
}

export function validateImage(file: File): string | undefined {
  if (!IMAGE_TYPES.includes(file.type)) return 'validation.imageType'
  if (file.size > MAX_IMAGE_BYTES) return 'validation.imageSize'
  return undefined
}
