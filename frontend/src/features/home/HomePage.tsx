import { useAuth } from '@/features/auth/auth-context'
import DashboardPage from './DashboardPage'
import LandingPage from './LandingPage'

export default function HomePage() {
  const { user } = useAuth()
  return user ? <DashboardPage /> : <LandingPage />
}
