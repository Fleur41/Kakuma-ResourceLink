import { describe, expect, it } from 'vitest'
import de from './locales/de.json'
import en from './locales/en.json'
import es from './locales/es.json'
import it_ from './locales/it.json'
import sw from './locales/sw.json'

type Tree = { [k: string]: string | Tree }
const PLURAL = /_(zero|one|two|few|many|other)$/

function flat(tree: Tree, prefix = ''): Record<string, string> {
  return Object.entries(tree).reduce<Record<string, string>>((acc, [k, v]) => {
    if (typeof v === 'string') acc[prefix + k] = v
    else Object.assign(acc, flat(v, `${prefix}${k}.`))
    return acc
  }, {})
}
const placeholders = (s: string) => [...s.matchAll(/{{\s*(\w+)\s*}}/g)].map((m) => m[1]).sort()

const base = flat(en as Tree)
const baseKeys = new Set(Object.keys(base).map((k) => k.replace(PLURAL, '')))

describe.each([
  ['sw', sw],
  ['es', es],
  ['it', it_],
  ['de', de],
])('%s translation', (_code, data) => {
  const strings = flat(data as Tree)
  const keys = new Set(Object.keys(strings).map((k) => k.replace(PLURAL, '')))

  it('has every English key and no extras', () => {
    expect([...baseKeys].filter((k) => !keys.has(k))).toEqual([])
    expect([...keys].filter((k) => !baseKeys.has(k))).toEqual([])
  })

  it('keeps the same placeholders and plural forms', () => {
    for (const [key, value] of Object.entries(strings)) {
      const enKey = key.replace(/_many$/, '_other')
      expect(placeholders(value), key).toEqual(placeholders(base[enKey]))
    }
    for (const key of Object.keys(base).filter((k) => k.endsWith('_other'))) expect(strings[key], key).toBeTruthy()
  })
})
