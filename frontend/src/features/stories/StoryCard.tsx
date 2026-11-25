import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Heart, MessageCircle } from 'lucide-react'
import type { StoryList } from '@/api/types'
import { Avatar } from '@/components/ui/Avatar'
import { Badge, StatusBadge } from '@/components/ui/Badge'
import { cn } from '@/lib/format'

export function StoryCover({ story, className, fit }: { story: { cover_image?: string | null }; className?: string; fit?: boolean }) {
  if (story.cover_image) return <img src={story.cover_image} alt="" loading="lazy" className={cn(fit ? 'object-contain' : 'object-cover', className)} />
  return (
    <div aria-hidden className={cn('hero-gradient flex items-end p-4', className)}>
      <span className="font-display text-4xl font-bold text-white/30">“</span>
    </div>
  )
}

export function StoryMeta({ story }: { story: StoryList }) {
  const { t } = useTranslation()
  return (
    <span className="text-sm text-muted">
      {[story.author.full_name, story.author.country, t('stories.readingTime', { count: story.reading_time })].filter(Boolean).join(' · ')}
    </span>
  )
}

export function StoryCard({ story, showStatus, horizontal }: { story: StoryList; showStatus?: boolean; horizontal?: boolean }) {
  const { t } = useTranslation()
  const href = `/stories/${story.id}`
  return (
    <article className={cn('card card-interactive group relative flex overflow-hidden', horizontal ? 'flex-row gap-4 p-4' : 'flex-col')}>
      {/* Whole photo as uploaded (no cropping): natural proportions in grid cards, fitted inside the square thumbnail. */}
      <div className={cn('overflow-hidden bg-surface', horizontal ? 'size-24 shrink-0 sm:size-28' : story.cover_image ? 'w-full' : 'aspect-[16/9] w-full')}>
        <StoryCover
          story={story}
          className={cn(
            'transition-transform duration-500 ease-out group-hover:scale-[1.04] motion-reduce:group-hover:scale-100',
            story.cover_image && !horizontal ? 'mx-auto block h-auto max-h-96 w-auto max-w-full' : 'size-full',
          )}
          fit={Boolean(story.cover_image)}
        />
      </div>
      <div className={cn('flex flex-1 flex-col gap-2', horizontal ? '' : 'p-5')}>
        <div className="flex flex-wrap gap-2">
          <Badge tone="gold">{t(`categories.story.${story.category}`)}</Badge>
          {showStatus && <StatusBadge status={story.status} />}
        </div>
        <h3 className="text-lg leading-snug font-normal">
          <Link to={href} className="after:absolute after:inset-0">
            {story.title}
          </Link>
        </h3>
        {!horizontal && story.excerpt && <p className="line-clamp-2 text-muted">{story.excerpt}</p>}
        <div className="mt-auto flex items-center gap-3 pt-2">
          <Avatar name={story.author.full_name} src={story.author.avatar} size={28} />
          <StoryMeta story={story} />
        </div>
        {!horizontal && story.status === 'published' && (
          <div className="flex gap-4 text-sm text-muted">
            <span className="flex items-center gap-1" aria-label={t('stories.likesCount', { count: story.likes_count })}>
              <Heart size={16} aria-hidden className={story.is_liked ? 'fill-error text-error' : ''} /> {story.likes_count}
            </span>
            <span className="flex items-center gap-1" aria-label={t('stories.commentsCount', { count: story.comments_count })}>
              <MessageCircle size={16} aria-hidden /> {story.comments_count}
            </span>
          </div>
        )}
      </div>
    </article>
  )
}
