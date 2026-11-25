import { Suspense, useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { Bell, Bot, ChevronDown, LogOut, Menu, ShieldCheck, UserRound, X } from 'lucide-react'
import { notificationsApi } from '@/api/endpoints'
import { Avatar } from '@/components/ui/Avatar'
import { OfflineBanner, PageLoader } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'
import { cn } from '@/lib/format'
import { useOnline } from '@/lib/hooks'
import { LanguageSelect } from './LanguageSelect'
import { memberNav, moderationNav, publicNav, type NavItem } from './nav'

/** Pages reached from the account menu and the top-bar icons rather than the main links. */
const UTILITY_ROUTES = new Set(['/assistant', '/notifications', '/profile'])

function SkipLink() {
  const { t } = useTranslation()
  return (
    <a href="#main" className="sr-only-focusable fixed start-4 top-4 z-50 bg-primary px-4 py-2 font-medium text-on-primary">
      {t('common.skipToContent')}
    </a>
  )
}

/** Menus belong to the page they were opened on, so navigating closes them without an effect. */
function useOpenOnPage() {
  const { pathname } = useLocation()
  const [openOn, setOpenOn] = useState<string | null>(null)
  const open = openOn === pathname
  return [open, (v: boolean) => setOpenOn(v ? pathname : null)] as const
}

function useSignOut() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  return async () => {
    await logout()
    toast.success(t('auth.signedOut'))
    navigate('/', { replace: true }) // public home, not a member page that would ask to sign in again
  }
}

/**
 * One shell for guests and members: a slim dark top bar with the main links, a full-screen menu below
 * the xl breakpoint, and a dark footer. Members get the assistant, notifications and an account menu.
 */
export function AppShell() {
  const online = useOnline()
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0 })
  }, [pathname])

  return (
    <div className="flex min-h-screen flex-col">
      <SkipLink />
      {!online && (
        <div className="sticky top-0 z-40">
          <OfflineBanner />
        </div>
      )}
      <TopBar />
      <main id="main" tabIndex={-1} className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-10 outline-none sm:px-6 lg:px-10 lg:py-14">
        {/* Keyed by path so each page fades in on navigation. */}
        <div key={pathname} className="animate-in">
          <Suspense fallback={<PageLoader />}>
            <Outlet />
          </Suspense>
        </div>
      </main>
      <Footer />
    </div>
  )
}

function Wordmark() {
  return (
    <Link to="/" className="flex shrink-0 items-center gap-2.5 pe-6 text-[15px] text-white">
      <img src="/favicon.svg" alt="" width={24} height={24} />
      <span>
        <span className="font-semibold tracking-[0.02em]">KAKUMA</span>
        <span className="ms-1.5 font-light text-white/75">ResourceLink</span>
      </span>
    </Link>
  )
}

function TopBar() {
  const { t } = useTranslation()
  const { user, isModerator } = useAuth()
  const [menuOpen, setMenuOpen] = useOpenOnPage()
  const signOut = useSignOut()

  const unread = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    enabled: !!user,
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  })
  const unreadCount = user ? (unread.data?.count ?? 0) : 0

  const links: NavItem[] = user
    ? [...memberNav.filter((i) => !UTILITY_ROUTES.has(i.to)), ...(isModerator ? [moderationNav] : [])]
    : publicNav

  useEffect(() => {
    if (!menuOpen) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setMenuOpen(false)
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [menuOpen, setMenuOpen])

  return (
    <header className="sticky top-0 z-30 bg-shell text-white">
      <div className="mx-auto flex h-14 max-w-[1440px] items-stretch px-4 lg:px-6">
        <Wordmark />
        <nav aria-label={t('nav.main')} className="hidden items-stretch border-s border-white/10 xl:flex">
          {links.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  'relative flex items-center px-4 text-sm transition-colors',
                  isActive
                    ? 'bg-white/[0.06] text-white after:absolute after:inset-x-0 after:bottom-0 after:h-[2px] after:bg-shell-accent'
                    : 'text-white/70 hover:bg-white/10 hover:text-white',
                )
              }
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </nav>

        <div className="ms-auto flex items-center gap-1">
          <div className="me-3 hidden md:block">
            <LanguageSelect tone="dark" id="lang-top" />
          </div>
          {user ? (
            <>
              <Link
                to="/assistant"
                className="me-1 hidden h-9 items-center gap-2 rounded-[4px] border border-white/30 px-3 text-sm text-white transition-colors hover:bg-white hover:text-shell sm:inline-flex"
              >
                <Bot size={16} aria-hidden />
                {t('nav.assistant')}
              </Link>
              <Link
                to="/notifications"
                className="relative inline-flex size-10 items-center justify-center text-white/80 transition-colors hover:bg-white/10 hover:text-white"
                aria-label={unreadCount ? `${t('nav.notifications')} – ${t('notifications.unreadCount', { count: unreadCount })}` : t('nav.notifications')}
              >
                <Bell size={18} aria-hidden />
                {unreadCount > 0 && (
                  <span aria-hidden className="absolute end-1 top-1.5 min-w-4 bg-gold px-1 text-center text-[10px] leading-4 font-semibold text-[#161616]">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                )}
              </Link>
              <AccountMenu onSignOut={signOut} />
            </>
          ) : (
            <>
              <Link to="/login" className="hidden h-9 items-center px-3 text-sm text-white/80 transition-colors hover:text-white sm:inline-flex">
                {t('auth.signIn')}
              </Link>
              <Link to="/register" className="inline-flex h-9 items-center rounded-[4px] bg-white px-4 text-sm font-medium text-[#161616] transition-colors hover:bg-[#e0e0e0]">
                {t('auth.joinUs')}
              </Link>
            </>
          )}
          <button
            type="button"
            aria-label={menuOpen ? t('nav.closeMenu') : t('nav.openMenu')}
            aria-expanded={menuOpen}
            aria-controls="site-menu"
            onClick={() => setMenuOpen(!menuOpen)}
            className="ms-1 inline-flex size-10 items-center justify-center text-white/80 transition-colors hover:bg-white/10 hover:text-white xl:hidden"
          >
            {menuOpen ? <X size={20} aria-hidden /> : <Menu size={20} aria-hidden />}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav id="site-menu" aria-label={t('nav.main')} className="fixed inset-x-0 top-14 bottom-0 z-30 overflow-y-auto bg-surface text-text xl:hidden">
          <div className="animate-in mx-auto max-w-xl px-6 py-8">
            <ul>
              {[...links, ...(user ? memberNav.filter((i) => UTILITY_ROUTES.has(i.to)) : [])].map((item) => (
                <li key={item.to} className="border-b border-border">
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      cn('flex items-center justify-between py-4 text-[22px] font-light transition-colors', isActive ? 'text-primary' : 'hover:text-primary')
                    }
                  >
                    {t(item.labelKey)}
                    <item.icon size={20} aria-hidden className="text-muted" />
                  </NavLink>
                </li>
              ))}
            </ul>
            <div className="mt-8 flex flex-col gap-3">
              <LanguageSelect id="lang-menu" />
              {user ? (
                <button type="button" onClick={() => void signOut()} className="flex min-h-12 items-center justify-center gap-2 rounded-[4px] border border-text/70 font-medium">
                  <LogOut size={18} aria-hidden />
                  {t('auth.signOut')}
                </button>
              ) : (
                <>
                  <Link to="/register" className="flex min-h-12 items-center justify-center rounded-[4px] bg-primary font-medium text-on-primary">
                    {t('auth.joinUs')}
                  </Link>
                  <Link to="/login" className="flex min-h-12 items-center justify-center rounded-[4px] border border-text/70 font-medium">
                    {t('auth.signIn')}
                  </Link>
                </>
              )}
            </div>
          </div>
        </nav>
      )}
    </header>
  )
}

function AccountMenu({ onSignOut }: { onSignOut: () => Promise<void> }) {
  const { t } = useTranslation()
  const { user, isModerator } = useAuth()
  const [open, setOpen] = useOpenOnPage()
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false)
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, setOpen])

  const item = 'flex min-h-11 w-full items-center gap-3 px-4 text-sm text-white/85 transition-colors hover:bg-white/10 hover:text-white'

  return (
    <div ref={ref} className="relative hidden sm:block">
      <button
        type="button"
        aria-label={t('nav.profile')}
        aria-expanded={open}
        aria-controls="account-menu"
        onClick={() => setOpen(!open)}
        className={cn('flex h-14 items-center gap-2 px-2 transition-colors hover:bg-white/10', open && 'bg-shell-raised')}
      >
        <Avatar name={user?.full_name} src={user?.profile?.avatar} size={30} />
        <ChevronDown size={14} aria-hidden className={cn('text-white/60 transition-transform', open && 'rotate-180')} />
      </button>
      {open && (
        <div id="account-menu" className="animate-in absolute end-0 top-full w-72 border border-white/10 bg-shell-raised text-white">
          <div className="border-b border-white/10 px-4 py-4">
            <p className="truncate font-medium">{user?.full_name}</p>
            <p className="truncate text-xs text-white/60">{t(`roles.${user?.role ?? 'member'}`)}</p>
          </div>
          <div className="py-1">
            <Link to="/profile" className={item}>
              <UserRound size={16} aria-hidden /> {t('nav.profile')}
            </Link>
            {isModerator && (
              <Link to="/moderation" className={item}>
                <ShieldCheck size={16} aria-hidden /> {t('nav.moderation')}
              </Link>
            )}
          </div>
          <div className="border-t border-white/10 py-1">
            <button type="button" onClick={() => void onSignOut()} className={item}>
              <LogOut size={16} aria-hidden /> {t('auth.signOut')}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function Footer() {
  const { t } = useTranslation()
  return (
    <footer className="bg-shell text-white/70">
      <div className="mx-auto grid max-w-[1440px] gap-10 px-4 py-14 text-sm sm:px-6 md:grid-cols-[1.2fr_1fr_1.2fr] lg:px-10">
        <div className="space-y-4">
          <Wordmark />
          <p className="max-w-sm leading-relaxed">{t('brand.tagline')}</p>
        </div>
        <ul className="space-y-2.5">
          {publicNav.map((item) => (
            <li key={item.to}>
              <Link to={item.to} className="transition-colors hover:text-white">
                {t(item.labelKey)}
              </Link>
            </li>
          ))}
        </ul>
        <div className="space-y-3 leading-relaxed">
          <p>{t('footer.tagline')}</p>
          <p className="text-xs text-white/50">{t('footer.sampleNotice')}</p>
        </div>
      </div>
    </footer>
  )
}
