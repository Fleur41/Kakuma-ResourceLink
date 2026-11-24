import { describe, expect, it } from 'vitest'
import { daysUntil, initials, parseTags, wordCount } from './format'

describe('format helpers', () => {
  it('counts whole days until a deadline', () => {
    const today = new Date(2026, 10, 24)
    expect(daysUntil('2026-11-24', today)).toBe(0)
    expect(daysUntil('2026-11-30', today)).toBe(6)
    expect(daysUntil('2026-11-20', today)).toBe(-4)
  })

  it('parses comma separated tags, trimming and de-duplicating case-insensitively', () => {
    expect(parseTags(' Python, python ,  , Career advice,')).toEqual(['Python', 'Career advice'])
    expect(parseTags(Array.from({ length: 30 }, (_, i) => `t${i}`).join(','))).toHaveLength(20)
  })

  it('counts words', () => {
    expect(wordCount('')).toBe(0)
    expect(wordCount('  one two\nthree  ')).toBe(3)
  })

  it('builds initials', () => {
    expect(initials('Amina Yusuf')).toBe('AY')
    expect(initials('Daniel')).toBe('D')
    expect(initials(undefined)).toBe('?')
  })
})
