import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { ArrowLeft, Briefcase, Mail, MapPin, UsersRound } from 'lucide-react'
import { alumniApi } from '@/api/endpoints'
import type { AlumniPublic } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge } from '@/components/ui/Badge'
import { Button, ButtonLink } from '@/components/ui/Button'
import { ErrorState, PageLoader } from '@/components/ui/feedback'
import { useAuth } from '@/features/auth/auth-context'
import { MentorshipRequestDialog } from '@/features/mentorship/MentorshipRequestDialog'
import { useDocumentTitle } from '@/lib/hooks'
import { useRequireMember } from '@/lib/useRequireMember'

function TagList({ title, items }: { title: string; items: string[] }) {
  if (!items.length) return null
  return (
    <div>
      <h3 className="mb-2 text-sm font-bold text-muted uppercase">{title}</h3>
      <ul className="flex flex-wrap gap-2">
        {items.map((i) => (
          <li key={i}>
            <Badge>{i}</Badge>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default function AlumniProfilePage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const { user } = useAuth()
  const requireMember = useRequireMember()
  const [open, setOpen] = useState(false)
  const query = useQuery({ queryKey: ['alumni', 'detail', id, Boolean(user)], queryFn: () => alumniApi.get(id) })
  useDocumentTitle(query.data?.full_name)

  if (query.isPending) return <PageLoader />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const a = query.data as AlumniPublic
  const isSelf = user?.id === a.user_id

  return (
    <div className="space-y-6">
      <Link to="/alumni" className="inline-flex min-h-11 items-center gap-2 font-semibold text-primary hover:underline">
        <ArrowLeft size={18} aria-hidden />
        {t('alumni.back')}
      </Link>
      <header className="card flex flex-col gap-6 p-6 sm:flex-row sm:items-center sm:p-8">
        <Avatar name={a.full_name} src={a.avatar} size={96} />
        <div className="min-w-0 flex-1 space-y-2">
          <h1 className="text-[28px] font-bold">{a.full_name}</h1>
          <p className="flex items-center gap-2 text-muted">
            <Briefcase size={18} aria-hidden />
            {[a.profession, a.organization].filter(Boolean).join(' · ')}
          </p>
          <p className="flex items-center gap-2 text-muted">
            <MapPin size={18} aria-hidden />
            {[a.city, a.country].filter(Boolean).join(', ')}
          </p>
          <div className="flex flex-wrap gap-2 pt-1">
            {a.is_mentor && <Badge tone="gold">{t('alumni.openToMentor')}</Badge>}
            {a.willing_to_contribute && <Badge>{t('alumni.willingToContribute')}</Badge>}
          </div>
        </div>
        <div className="flex flex-col gap-3">
          {isSelf ? (
            <ButtonLink to="/alumni/me" variant="secondary">
              {t('alumni.editMyProfile')}
            </ButtonLink>
          ) : (
            a.is_mentor && (
              <Button icon={<UsersRound size={18} aria-hidden />} onClick={() => requireMember(() => setOpen(true))}>
                {t('mentorship.request')}
              </Button>
            )
          )}
          {a.contact_email && (
            <a href={`mailto:${a.contact_email}`} className="inline-flex min-h-11 items-center gap-2 font-semibold break-all text-primary hover:underline">
              <Mail size={18} aria-hidden />
              {a.contact_email}
            </a>
          )}
        </div>
      </header>
      <section className="card space-y-6 p-6 sm:p-8">
        {a.bio && (
          <div>
            <h2 className="mb-2 text-xl font-semibold">{t('alumni.about')}</h2>
            <p className="whitespace-pre-line">{a.bio}</p>
          </div>
        )}
        <TagList title={t('alumni.skills')} items={a.skills} />
        <TagList title={t('alumni.mentorshipAreas')} items={a.mentorship_areas} />
        <TagList title={t('alumni.contributionInterests')} items={a.contribution_interests} />
      </section>
      {open && <MentorshipRequestDialog open onClose={() => setOpen(false)} mentorProfileId={a.id} mentorName={a.full_name} />}
    </div>
  )
}
