import { describe, expect, it } from 'vitest'
import { validateImage, validateStory } from './storyValidation'

describe('story validation', () => {
  it('lets a draft be saved without consent but not without a title or body', () => {
    expect(validateStory({ title: 'Hi', body: '', consent: false }, false)).toEqual({ title: 'validation.titleLength', body: 'validation.required' })
    expect(validateStory({ title: 'My journey', body: 'Text', consent: false }, false)).toEqual({})
  })

  it('requires consent to submit for review', () => {
    expect(validateStory({ title: 'My journey', body: 'Text', consent: false }, true)).toEqual({ consent: 'stories.editor.consentRequired' })
    expect(validateStory({ title: 'My journey', body: 'Text', consent: true }, true)).toEqual({})
  })

  it('accepts only small jpg/png/webp covers', () => {
    expect(validateImage(new File(['x'], 'a.gif', { type: 'image/gif' }))).toBe('validation.imageType')
    const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.png', { type: 'image/png' })
    expect(validateImage(big)).toBe('validation.imageSize')
    expect(validateImage(new File(['x'], 'a.webp', { type: 'image/webp' }))).toBeUndefined()
  })
})
