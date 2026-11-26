import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { PageHeader } from '@/components/ui/Page'
import { formatDate } from '@/lib/format'

/** Date the legal texts were last revised. Update it whenever their wording changes. */
const LAST_UPDATED = '2026-11-26'

type LegalDoc = 'privacy' | 'terms'

interface Section {
  title: string
  body: string
}

/** Privacy notice and terms of use: same layout, text from the `legal.<doc>` translations. */
export default function LegalPage({ doc }: { doc: LegalDoc }) {
  const { t } = useTranslation()
  const sections = Object.values(t(`legal.${doc}.sections`, { returnObjects: true }) as Record<string, Section>)
  const other: LegalDoc = doc === 'privacy' ? 'terms' : 'privacy'

  return (
    <article className="max-w-3xl">
      <PageHeader title={t(`legal.${doc}.title`)} subtitle={t('legal.lastUpdated', { date: formatDate(LAST_UPDATED, { dateStyle: 'long' }) })} />
      <p className="mb-10 text-[17px] leading-relaxed">{t(`legal.${doc}.intro`)}</p>
      <ol className="space-y-9">
        {sections.map((s, i) => (
          <li key={s.title}>
            <h2 className="mb-2 flex gap-3 text-xl font-medium">
              <span aria-hidden className="text-primary tabular-nums">
                {i + 1}.
              </span>
              {s.title}
            </h2>
            <p className="leading-relaxed text-muted">{s.body}</p>
          </li>
        ))}
      </ol>
      <p className="mt-12 border-t border-border pt-6 text-sm text-muted">
        {t('legal.seeAlso')}{' '}
        <Link to={`/${other}`} className="font-medium text-primary underline-offset-4 hover:underline">
          {t(`legal.${other}.title`)}
        </Link>
      </p>
    </article>
  )
}
