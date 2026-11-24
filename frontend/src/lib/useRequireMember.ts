import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router'
import { useToast } from '@/components/ui/toast-context'
import { useAuth } from '@/features/auth/auth-context'

/** Wraps a member-only action: guests get a short hint and go to sign in, then come back here. */
export function useRequireMember() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const { t } = useTranslation()
  return useCallback(
    (action: () => void) => {
      if (user) return action()
      toast.info(t('auth.signInToContinue'))
      navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`)
    },
    [user, navigate, location, toast, t],
  )
}
