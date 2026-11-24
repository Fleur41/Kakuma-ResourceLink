import { createContext, useContext } from 'react'
import type { Language, RegisterRole, User } from '@/api/types'

export interface RegisterInput {
  full_name: string
  email: string
  password: string
  role: RegisterRole
  preferred_language: Language
}

export interface AuthApi {
  user: User | null
  ready: boolean
  login: (email: string, password: string) => Promise<User>
  register: (input: RegisterInput) => Promise<User>
  logout: () => Promise<void>
  setUser: (user: User) => void
  setLanguage: (lang: Language) => void
  isModerator: boolean
}

export const AuthContext = createContext<AuthApi | null>(null)

export function useAuth(): AuthApi {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
