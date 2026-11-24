import { describe, expect, it } from 'vitest'
import { safeNext } from './guards'
import { validateLogin, validatePassword, validateRegister } from './validation'

describe('auth validation', () => {
  it('requires a valid email and a password to sign in', () => {
    expect(validateLogin('', '')).toEqual({ email: 'validation.required', password: 'validation.required' })
    expect(validateLogin('amina@', 'x')).toEqual({ email: 'validation.email' })
    expect(validateLogin(' amina@demo.kakuma.test ', 'secret')).toEqual({})
  })

  it('mirrors the backend password rules', () => {
    expect(validatePassword('short')).toBe('validation.passwordLength')
    expect(validatePassword('12345678')).toBe('validation.passwordNumeric')
    expect(validatePassword('kakuma-2026')).toBeUndefined()
  })

  it('checks registration fields together', () => {
    const errors = validateRegister({ full_name: 'A', email: 'bad', password: 'kakuma-2026', confirm: 'other', terms: false })
    expect(errors).toEqual({ full_name: 'validation.name', email: 'validation.email', confirm: 'validation.passwordMatch', terms: 'validation.terms' })
  })

  it('only allows same-site redirects after sign-in', () => {
    expect(safeNext('/stories/new')).toBe('/stories/new')
    expect(safeNext('//evil.example')).toBe('/')
    expect(safeNext('https://evil.example')).toBe('/')
    expect(safeNext('/\\evil.example')).toBe('/')
    expect(safeNext(null)).toBe('/')
  })
})
