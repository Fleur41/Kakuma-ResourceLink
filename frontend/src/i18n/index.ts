import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import { setLanguageGetter } from '@/api/client'
import type { Language } from '@/api/types'
import de from './locales/de.json'
import en from './locales/en.json'
import es from './locales/es.json'
import it from './locales/it.json'
import sw from './locales/sw.json'

export const LANGUAGES: { code: Language; label: string }[] = [
  { code: 'en', label: 'English' },
  { code: 'sw', label: 'Kiswahili' },
  { code: 'es', label: 'Español' },
  { code: 'it', label: 'Italiano' },
  { code: 'de', label: 'Deutsch' },
]

export const LANGUAGE_KEY = 'kk.lang'

export function currentLanguage(): Language {
  const code = (i18n.resolvedLanguage ?? i18n.language ?? 'en').slice(0, 2)
  return (LANGUAGES.some((l) => l.code === code) ? code : 'en') as Language
}

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { en: { translation: en }, sw: { translation: sw }, es: { translation: es }, it: { translation: it }, de: { translation: de } },
    fallbackLng: 'en',
    supportedLngs: LANGUAGES.map((l) => l.code),
    nonExplicitSupportedLngs: true,
    interpolation: { escapeValue: false },
    detection: { order: ['localStorage', 'navigator'], lookupLocalStorage: LANGUAGE_KEY, caches: ['localStorage'] },
    returnNull: false,
  })

i18n.on('languageChanged', () => {
  document.documentElement.lang = currentLanguage()
})
document.documentElement.lang = currentLanguage()
setLanguageGetter(currentLanguage)

export default i18n
