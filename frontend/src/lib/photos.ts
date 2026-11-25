/**
 * Background photo slots. Each one shows `public/images/<slot>.webp` when that file exists and falls back
 * to the plain dark panel when it doesn't, so the app never shows a broken image.
 *
 * Add or replace a photo with `scripts/optimize-photos.sh <photo> <slot>` (resizes, compresses and strips
 * location data), then put the photographer's name in `credit`. Only use photos of people who agreed to
 * be photographed and to appear on the platform.
 */
export interface Photo {
  src: string
  /** Shown in the corner of the photo. Leave empty only for photos you took yourself. */
  credit: string
  /** CSS object-position, to keep faces in frame when the panel crops the photo. */
  position?: string
}

export const photos = {
  /** Landing page hero: wide shot of Kakuma town or its people. */
  hero: { src: '/images/hero.webp', credit: 'Random Institute / Unsplash', position: 'center 30%' },
  /** Dashboard banner. */
  dashboard: { src: '/images/dashboard.webp', credit: 'RUT MIIT / Unsplash', position: 'center 30%' },
  /** "Join the community" section at the bottom of the landing page. */
  community: { src: '/images/community.webp', credit: 'Vitaly Gariev / Unsplash', position: 'center 35%' },
} satisfies Record<string, Photo>

export type PhotoSlot = keyof typeof photos
