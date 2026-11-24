import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { PageLoader } from '@/components/ui/feedback'
import { useAuth } from './auth-context'

/** Member-only pages: guests are sent to sign in and brought back afterwards. */
export function RequireAuth({ children, moderator }: { children: ReactNode; moderator?: boolean }) {
  const { user, ready, isModerator } = useAuth()
  const location = useLocation()
  if (!ready) return <PageLoader />
  if (!user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />
  if (moderator && !isModerator) return <Navigate to="/" replace />
  return <>{children}</>
}

/** Login/register: already signed-in users go straight to the app. */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()
  if (user) return <Navigate to={safeNext(new URLSearchParams(location.search).get('next'))} replace />
  return <>{children}</>
}

/** Only same-site relative paths are allowed as a post-login redirect (no open redirects). */
export function safeNext(next: string | null | undefined): string {
  if (!next || !next.startsWith('/') || next.startsWith('//') || next.startsWith('/\\')) return '/'
  return next
}
