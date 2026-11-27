import { DE, ES, GB, IT, KE, type FlagComponent } from 'country-flag-icons/react/3x2'
import type { Language } from '@/api/types'

/** Kiswahili uses Kenya's flag: Kakuma is in Kenya. SVG flags, because emoji flags show as letters on Windows. */
const FLAGS: Record<Language, FlagComponent> = { en: GB, sw: KE, es: ES, it: IT, de: DE }

export function LanguageFlag({ code }: { code: Language }) {
  const F = FLAGS[code]
  return <F aria-hidden className="h-4 w-6 shrink-0 rounded-[2px] shadow-[0_0_0_1px_rgba(0,0,0,0.15)]" />
}
