import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link, type LinkProps } from 'react-router'
import { cn } from '@/lib/format'
import { Spinner } from './feedback'

export type ButtonVariant = 'primary' | 'secondary' | 'tonal' | 'text' | 'danger' | 'inverse' | 'outlineInverse'
type Size = 'sm' | 'md' | 'lg'

const base =
  'inline-flex items-center justify-center gap-2 rounded-[4px] font-medium tracking-[0.01em] transition-colors duration-150 select-none text-center ' +
  'disabled:opacity-40 disabled:cursor-not-allowed aria-disabled:opacity-40 aria-disabled:pointer-events-none'

const variants: Record<ButtonVariant, string> = {
  primary: 'bg-primary text-on-primary hover:bg-primary-strong',
  secondary: 'border border-text/70 text-text bg-transparent hover:bg-text hover:text-surface',
  tonal: 'bg-hover text-text hover:bg-border',
  text: 'text-primary hover:bg-primary-container',
  danger: 'bg-error text-white hover:brightness-110',
  inverse: 'bg-white text-[#161616] hover:bg-[#e0e0e0]',
  /** White outline for dark panels and photos. */
  outlineInverse: 'border border-white/70 bg-transparent text-white hover:bg-white hover:text-[#161616]',
}

const sizes: Record<Size, string> = {
  sm: 'min-h-9 px-3 text-sm',
  md: 'min-h-11 px-5 text-[15px]',
  lg: 'min-h-12 min-w-[200px] px-8 text-[15px]',
}

export function buttonClass(variant: ButtonVariant = 'primary', size: Size = 'md', extra?: string) {
  return cn(base, variants[variant], sizes[size], extra)
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: Size
  loading?: boolean
  icon?: ReactNode
}

/** Loading keeps the label (so width and meaning stay) and marks the button busy for screen readers. */
export function Button({ variant = 'primary', size = 'md', loading, icon, className, children, disabled, type = 'button', ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonClass(variant, size, className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <Spinner size={16} /> : icon}
      {children}
    </button>
  )
}

interface ButtonLinkProps extends LinkProps {
  variant?: ButtonVariant
  size?: Size
  icon?: ReactNode
}

export function ButtonLink({ variant = 'primary', size = 'md', icon, className, children, ...rest }: ButtonLinkProps) {
  return (
    <Link className={buttonClass(variant, size, typeof className === 'string' ? className : undefined)} {...rest}>
      {icon}
      {children}
    </Link>
  )
}

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  label: string
  pressed?: boolean
}

export function IconButton({ label, pressed, className, children, type = 'button', ...rest }: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      aria-pressed={pressed}
      className={cn(
        'inline-flex size-11 shrink-0 items-center justify-center rounded-full text-muted transition-colors hover:bg-primary-container hover:text-primary disabled:opacity-40',
        className,
      )}
      {...rest}
    >
      {children}
    </button>
  )
}
