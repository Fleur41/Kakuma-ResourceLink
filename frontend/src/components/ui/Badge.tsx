import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { BadgeCheck } from 'lucide-react'
import { cn } from '@/lib/format'

type Tone = 'neutral' | 'gold' | 'accent' | 'error' | 'primary'

const tones: Record<Tone, string> = {
  neutral: 'bg-border/60 text-muted',
  gold: 'bg-gold-container text-gold-text',
  accent: 'bg-primary-container text-primary',
  error: 'bg-error/10 text-error',
  primary: 'bg-primary text-on-primary',
}

export function Badge({ tone = 'accent', children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold whitespace-nowrap', tones[tone], className)}>
      {children}
    </span>
  )
}

const statusTone: Record<string, Tone> = {
  draft: 'neutral',
  pending_review: 'gold',
  pending: 'gold',
  published: 'accent',
  active: 'accent',
  accepted: 'accent',
  completed: 'primary',
  rejected: 'error',
  declined: 'error',
  cancelled: 'neutral',
  archived: 'neutral',
  pledged: 'gold',
}

/** Moderation / request status, always shown to the author (design rule). */
export function StatusBadge({ status }: { status: string }) {
  const { t } = useTranslation()
  return <Badge tone={statusTone[status] ?? 'neutral'}>{t(`status.${status}`, { defaultValue: status })}</Badge>
}

/** Seed data is never verified; only show the check for records a moderator verified. */
export function VerifiedBadge({ verified }: { verified?: boolean }) {
  const { t } = useTranslation()
  if (!verified) return <Badge tone="neutral">{t('common.unverified')}</Badge>
  return (
    <Badge tone="accent">
      <BadgeCheck size={14} aria-hidden />
      {t('common.verified')}
    </Badge>
  )
}

interface ChipProps {
  selected: boolean
  onClick: () => void
  children: ReactNode
}

export function Chip({ selected, onClick, children }: ChipProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        'min-h-10 rounded-full border px-4 text-sm font-medium whitespace-nowrap transition-colors',
        selected ? 'border-primary bg-primary text-on-primary' : 'border-border bg-surface text-text hover:border-primary',
      )}
    >
      {children}
    </button>
  )
}
