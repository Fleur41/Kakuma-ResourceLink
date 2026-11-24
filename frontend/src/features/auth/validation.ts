export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

export type FieldErrors<K extends string> = Partial<Record<K, string>>

/** Returns i18n keys (not text) so the rules can be unit tested without a translator. */
export function validateLogin(email: string, password: string): FieldErrors<'email' | 'password'> {
  const e: FieldErrors<'email' | 'password'> = {}
  if (!email.trim()) e.email = 'validation.required'
  else if (!EMAIL_RE.test(email.trim())) e.email = 'validation.email'
  if (!password) e.password = 'validation.required'
  return e
}

export function validatePassword(password: string): string | undefined {
  if (!password) return 'validation.required'
  if (password.length < 8) return 'validation.passwordLength'
  if (/^\d+$/.test(password)) return 'validation.passwordNumeric'
  return undefined
}

export function validateRegister(v: { full_name: string; email: string; password: string; confirm: string; terms: boolean }) {
  const e: FieldErrors<'full_name' | 'email' | 'password' | 'confirm' | 'terms'> = {}
  if (v.full_name.trim().length < 2) e.full_name = 'validation.name'
  if (!v.email.trim()) e.email = 'validation.required'
  else if (!EMAIL_RE.test(v.email.trim())) e.email = 'validation.email'
  const pw = validatePassword(v.password)
  if (pw) e.password = pw
  if (v.confirm !== v.password) e.confirm = 'validation.passwordMatch'
  if (!v.terms) e.terms = 'validation.terms'
  return e
}
