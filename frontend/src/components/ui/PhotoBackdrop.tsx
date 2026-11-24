import { useState } from 'react'
import { Camera } from 'lucide-react'
import { cn } from '@/lib/format'
import { photos, type PhotoSlot } from '@/lib/photos'

/**
 * Full-bleed background photo for a dark panel. The parent must be `relative overflow-hidden` and its
 * content `relative z-10`. A missing file (the dev server and nginx answer with index.html) fails to
 * decode, so the panel quietly keeps its plain background.
 */
export function PhotoBackdrop({ slot, priority }: { slot: PhotoSlot; priority?: boolean }) {
  const photo = photos[slot]
  const [state, setState] = useState<'loading' | 'ok' | 'missing'>('loading')
  if (state === 'missing') return null

  return (
    <>
      <img
        src={photo.src}
        alt=""
        decoding="async"
        loading={priority ? 'eager' : 'lazy'}
        fetchPriority={priority ? 'high' : 'auto'}
        onLoad={() => setState('ok')}
        onError={() => setState('missing')}
        style={{ objectPosition: photo.position }}
        className={cn('absolute inset-0 size-full object-cover transition-opacity duration-700', state === 'ok' ? 'opacity-100' : 'opacity-0')}
      />
      {/* Darkens the photo so white text stays readable (WCAG contrast) whatever the picture. */}
      <div
        aria-hidden
        className={cn(
          'absolute inset-0 bg-[linear-gradient(180deg,rgba(0,0,0,0.45)_0%,rgba(0,0,0,0.35)_45%,rgba(0,0,0,0.78)_100%)] transition-opacity duration-700',
          state === 'ok' ? 'opacity-100' : 'opacity-0',
        )}
      />
      {state === 'ok' && photo.credit && (
        <p className="absolute end-4 bottom-3 z-10 flex items-center gap-1.5 text-[11px] text-white/65">
          <Camera size={12} aria-hidden />
          {photo.credit}
        </p>
      )}
    </>
  )
}
