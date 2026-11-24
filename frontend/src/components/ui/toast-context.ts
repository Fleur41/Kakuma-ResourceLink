import { createContext, useContext } from 'react'

export type ToastKind = 'success' | 'error' | 'info'
export interface ToastAction {
  label: string
  onClick: () => void
}
export interface ToastApi {
  show: (message: string, opts?: { kind?: ToastKind; action?: ToastAction; duration?: number }) => void
  success: (message: string) => void
  error: (message: string, action?: ToastAction) => void
  info: (message: string) => void
}

export const ToastContext = createContext<ToastApi | null>(null)

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>')
  return ctx
}
