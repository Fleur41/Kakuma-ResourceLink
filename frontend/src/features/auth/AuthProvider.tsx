import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router'
import { ApiError, onSessionExpired } from '@/api/client'
import { authApi } from '@/api/endpoints'
import { tokenStore } from '@/api/tokens'
import type { Language, User } from '@/api/types'
import { CACHE_KEY } from '@/app/queryClient'
import { useToast } from '@/components/ui/toast-context'
import i18n from '@/i18n'
import { AuthContext, type AuthApi, type RegisterInput } from './auth-context'

const USER_KEY = 'kk.user'

function readCachedUser(): User | null {
  if (!tokenStore.hasSession()) return null
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

function cacheUser(user: User | null) {
  try {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
    else localStorage.removeItem(USER_KEY)
  } catch {
    /* ignore */
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  // The cached profile lets a returning user open the app offline; it is refreshed from /auth/me below.
  const [user, setUserState] = useState<User | null>(readCachedUser)
  const [ready, setReady] = useState(() => !tokenStore.hasSession() || readCachedUser() !== null)

  const setUser = useCallback((u: User | null) => {
    setUserState(u)
    cacheUser(u)
  }, [])

  const resetData = useCallback(() => {
    queryClient.clear()
    try {
      localStorage.removeItem(CACHE_KEY)
    } catch {
      /* ignore */
    }
  }, [queryClient])

  // Restore the session after a reload.
  useEffect(() => {
    if (!tokenStore.hasSession()) return
    let cancelled = false
    authApi
      .me()
      .then((u) => {
        if (!cancelled) setUser(u)
      })
      .catch((e: unknown) => {
        if (cancelled) return
        if (e instanceof ApiError && (e.status === 401 || e.status === 403)) {
          tokenStore.clear()
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) setReady(true)
      })
    return () => {
      cancelled = true
    }
  }, [setUser])

  // Refresh token rejected: drop local data, tell the user briefly, send them to sign in.
  useEffect(
    () =>
      onSessionExpired(() => {
        setUser(null)
        resetData()
        toast.info(t('auth.sessionExpired'))
        const next = location.pathname + location.search
        navigate(`/login?next=${encodeURIComponent(next)}`, { replace: true })
      }),
    [navigate, location, resetData, setUser, t, toast],
  )

  const afterAuth = useCallback(
    (u: User, tokens: { access: string; refresh: string }) => {
      tokenStore.set(tokens)
      resetData() // anonymous cache has no bookmark/like state for this user
      setUser(u)
      if (u.preferred_language && u.preferred_language !== i18n.resolvedLanguage) void i18n.changeLanguage(u.preferred_language)
      return u
    },
    [resetData, setUser],
  )

  const login = useCallback<AuthApi['login']>(
    async (email, password) => {
      const res = await authApi.login({ email: email.trim().toLowerCase(), password })
      return afterAuth(res.user, res.tokens)
    },
    [afterAuth],
  )

  const register = useCallback(
    async (input: RegisterInput) => {
      const res = await authApi.register({ ...input, email: input.email.trim().toLowerCase(), full_name: input.full_name.trim() })
      return afterAuth(res.user, res.tokens)
    },
    [afterAuth],
  )

  const logout = useCallback(async () => {
    const refresh = tokenStore.refresh
    if (refresh) {
      try {
        await authApi.logout(refresh) // blacklists the refresh token server-side
      } catch {
        /* offline: the token still expires; local data is cleared below */
      }
    }
    tokenStore.clear()
    setUser(null)
    resetData()
  }, [resetData, setUser])

  const setLanguage = useCallback(
    (lang: Language) => {
      void i18n.changeLanguage(lang)
      if (user && user.preferred_language !== lang) {
        // Server-side notifications and messages follow this preference too.
        authApi
          .updateMe({ preferred_language: lang })
          .then(setUser)
          .catch(() => undefined)
      }
    },
    [user, setUser],
  )

  const value = useMemo<AuthApi>(
    () => ({
      user,
      ready,
      login,
      register,
      logout,
      setUser,
      setLanguage,
      isModerator: user?.role === 'moderator' || user?.role === 'admin',
    }),
    [user, ready, login, register, logout, setUser, setLanguage],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
