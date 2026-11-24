import { Suspense, useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import { Bell, LogOut, Menu, X } from 'lucide-react'
import { notificationsApi } from '@/api/endpoints'
import { buttonClass, IconButton } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/Avatar'
import { OfflineBanner, PageLoader } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { cn } from '@/lib/format'
import { useOnline } from '@/lib/hooks'
import { LanguageSelect } from './LanguageSelect'
import { Logo } from './Logo'
import { memberNav, moderationNav, publicNav } from './nav'

function SkipLink() {
  const { t } = useTranslation()
  return (
    <a href="#main" className="sr-only-focusable fixed start-4 top-4 z-50 rounded-md bg-primary px-4 py-2 font-semibold text-on-primary">
      {t('common.skipToContent')}
    </a>
  )
}

/** Guests get the public top navigation; members get the dashboard sidebar (design system §2 Navigation). */
export function AppShell() {
  const { user } = useAuth()
  const online = useOnline()
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0 })
  }, [pathname])

  return (
    <>
      <SkipLink />
      {!online && (
        <div className="sticky top-0 z-40">
          <OfflineBanner />
        </div>
      )}
      {user ? <MemberLayout /> : <PublicLayout />}
    </>
  )
}

function Content() {
  return (
    <main id="main" tabIndex={-1} className="mx-auto w-full max-w-[1200px] px-4 py-8 outline-none sm:px-6 lg:px-10">
      <Suspense fallback={<PageLoader />}>
        <Outlet />
      </Suspense>
    </main>
  )
}

function PublicLayout() {
  const { t } = useTranslation()
  // The menu is tied to the page it was opened on, so navigating closes it without an effect.
  const { pathname } = useLocation()
  const [openOn, setOpenOn] = useState<string | null>(null)
  const open = openOn === pathname
  const setOpen = (v: boolean | ((o: boolean) => boolean)) => setOpenOn((typeof v === 'function' ? v(open) : v) ? pathname : null)

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cn('rounded-md px-3 py-2 font-semibold transition-colors hover:text-primary', isActive ? 'text-primary' : 'text-text')

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-30 border-b border-border bg-surface/95 backdrop-blur">
        <div className="mx-auto flex h-[72px] max-w-[1200px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-10">
          <Logo />
          <nav aria-label={t('nav.main')} className="hidden items-center gap-1 lg:flex">
            {publicNav.map((item) => (
              <NavLink key={item.to} to={item.to} className={linkClass}>
                {t(item.labelKey)}
              </NavLink>
            ))}
          </nav>
          <div className="hidden items-center gap-3 lg:flex">
            <LanguageSelect id="lang-top" />
            <Link to="/login" className={buttonClass('text', 'md')}>
              {t('auth.signIn')}
            </Link>
            <Link to="/register" className={buttonClass('primary', 'md')}>
              {t('auth.joinUs')}
            </Link>
          </div>
          <IconButton label={open ? t('nav.closeMenu') : t('nav.openMenu')} aria-expanded={open} aria-controls="mobile-nav" onClick={() => setOpen((v) => !v)} className="lg:hidden">
            {open ? <X aria-hidden /> : <Menu aria-hidden />}
          </IconButton>
        </div>
        {open && (
          <nav id="mobile-nav" aria-label={t('nav.main')} className="border-t border-border bg-surface px-4 pb-5 lg:hidden">
            <ul className="flex flex-col py-2">
              {publicNav.map((item) => (
                <li key={item.to}>
                  <NavLink to={item.to} className={({ isActive }) => cn('flex min-h-12 items-center gap-3 rounded-md px-3 font-semibold', isActive ? 'text-primary' : 'text-text')}>
                    <item.icon size={20} aria-hidden />
                    {t(item.labelKey)}
                  </NavLink>
                </li>
              ))}
            </ul>
            <div className="flex flex-wrap gap-3">
              <LanguageSelect id="lang-mobile" />
              <Link to="/login" className={buttonClass('secondary', 'md', 'flex-1')}>
                {t('auth.signIn')}
              </Link>
              <Link to="/register" className={buttonClass('primary', 'md', 'flex-1')}>
                {t('auth.joinUs')}
              </Link>
            </div>
          </nav>
        )}
      </header>
      <div className="flex-1">
        <Content />
      </div>
      <Footer />
    </div>
  )
}

function Footer() {
  const { t } = useTranslation()
  return (
    <footer className="border-t border-border bg-surface">
      <div className="mx-auto flex max-w-[1200px] flex-col gap-2 px-4 py-6 text-sm text-muted sm:px-6 lg:px-10">
        <p>{t('footer.tagline')}</p>
        <p>{t('footer.sampleNotice')}</p>
      </div>
    </footer>
  )
}

function MemberLayout() {
  const { t } = useTranslation()
  const { user, logout, isModerator } = useAuth()
  const toast = useToast()
  const { pathname } = useLocation()
  const [openOn, setOpenOn] = useState<string | null>(null)
  const open = openOn === pathname
  const setOpen = (v: boolean) => setOpenOn(v ? pathname : null)

  const unread = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  })
  const unreadCount = unread.data?.count ?? 0

  const items = isModerator ? [...memberNav, moderationNav] : memberNav

  const sidebar = (
    <div className="flex h-full flex-col bg-[#075B4B] text-white">
      <div className="flex h-[72px] items-center px-5">
        <Logo inverse />
      </div>
      <nav aria-label={t('nav.main')} className="flex-1 overflow-y-auto px-3 py-2">
        <ul className="space-y-1">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'flex min-h-11 items-center gap-3 rounded-md px-3 text-[15px] font-medium transition-colors',
                    isActive ? 'bg-white text-[#075B4B]' : 'text-white/90 hover:bg-white/10',
                  )
                }
              >
                <item.icon size={20} aria-hidden />
                <span className="flex-1">{t(item.labelKey)}</span>
                {item.to === '/notifications' && unreadCount > 0 && (
                  <span className="rounded-full bg-[#F5A623] px-2 text-xs font-bold text-[#3d2800]" aria-label={t('notifications.unreadCount', { count: unreadCount })}>
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="space-y-3 border-t border-white/15 p-4">
        <LanguageSelect tone="dark" id="lang-side" />
        <div className="flex items-center gap-3">
          <Avatar name={user?.full_name} src={user?.profile?.avatar} size={36} />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold">{user?.full_name}</p>
            <p className="truncate text-xs text-white/70">{t(`roles.${user?.role ?? 'member'}`)}</p>
          </div>
          <IconButton
            label={t('auth.signOut')}
            className="text-white/80 hover:bg-white/10 hover:text-white"
            onClick={async () => {
              await logout()
              toast.success(t('auth.signedOut'))
            }}
          >
            <LogOut size={18} aria-hidden />
          </IconButton>
        </div>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen lg:ps-[264px]">
      <aside className="fixed inset-y-0 start-0 z-30 hidden w-[264px] lg:block">{sidebar}</aside>
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-surface px-4 lg:hidden">
        <IconButton label={t('nav.openMenu')} aria-expanded={open} aria-controls="drawer" onClick={() => setOpen(true)}>
          <Menu aria-hidden />
        </IconButton>
        <Logo />
        <Link
          to="/notifications"
          className="relative inline-flex size-11 items-center justify-center rounded-full text-muted hover:bg-primary-container hover:text-primary"
          aria-label={unreadCount ? `${t('nav.notifications')} – ${t('notifications.unreadCount', { count: unreadCount })}` : t('nav.notifications')}
        >
          <Bell size={20} aria-hidden />
          {unreadCount > 0 && <span aria-hidden className="absolute end-2 top-2 size-2.5 rounded-full bg-[#F5A623] ring-2 ring-surface" />}
        </Link>
      </header>
      {open && (
        <div id="drawer" className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label={t('nav.main')}>
          <button type="button" aria-label={t('nav.closeMenu')} className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 start-0 w-[min(86vw,300px)] shadow-2xl">{sidebar}</div>
        </div>
      )}
      <Content />
    </div>
  )
}
