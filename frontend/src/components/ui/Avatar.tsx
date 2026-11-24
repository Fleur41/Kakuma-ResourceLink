import { cn, initials } from '@/lib/format'

export function Avatar({ name, src, size = 40, className }: { name?: string | null; src?: string | null; size?: number; className?: string }) {
  const style = { width: size, height: size, fontSize: Math.max(12, size * 0.38) }
  if (src) return <img src={src} alt="" style={style} className={cn('shrink-0 rounded-full object-cover', className)} />
  return (
    <span aria-hidden style={style} className={cn('inline-flex shrink-0 items-center justify-center rounded-full bg-primary-container font-display font-semibold text-primary', className)}>
      {initials(name)}
    </span>
  )
}
