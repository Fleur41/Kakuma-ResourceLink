import { useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'
import { useTranslation } from 'react-i18next'
import { Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/format'

const control =
  'w-full rounded-none border bg-surface px-4 py-3 text-[15px] text-text placeholder:text-muted/80 transition-colors ' +
  'hover:border-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-60'

interface FieldShellProps {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  required?: boolean
  children: ReactNode
}

function FieldShell({ id, label, error, hint, required, children }: FieldShellProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-text">
        {label}
        {required && <span aria-hidden className="text-error"> *</span>}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-sm text-error">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-sm text-muted">
          {hint}
        </p>
      ) : null}
    </div>
  )
}

function describedBy(id: string, error?: string, hint?: ReactNode) {
  return error ? `${id}-error` : hint ? `${id}-hint` : undefined
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: ReactNode
}

export function TextField({ label, error, hint, id: idProp, className, required, ...rest }: InputProps) {
  const auto = useId()
  const id = idProp ?? auto
  return (
    <FieldShell id={id} label={label} error={error} hint={hint} required={required}>
      <input
        id={id}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(control, error ? 'border-error' : 'border-border', className)}
        {...rest}
      />
    </FieldShell>
  )
}

export function PasswordField({ label, error, hint, id: idProp, required, ...rest }: Omit<InputProps, 'type'>) {
  const { t } = useTranslation()
  const auto = useId()
  const id = idProp ?? auto
  const [visible, setVisible] = useState(false)
  return (
    <FieldShell id={id} label={label} error={error} hint={hint} required={required}>
      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(id, error, hint)}
          className={cn(control, 'pe-12', error ? 'border-error' : 'border-border')}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? t('auth.hidePassword') : t('auth.showPassword')}
          aria-pressed={visible}
          className="absolute inset-y-0 end-0 flex w-12 items-center justify-center rounded-e-md text-muted hover:text-primary"
        >
          {visible ? <EyeOff size={18} aria-hidden /> : <Eye size={18} aria-hidden />}
        </button>
      </div>
    </FieldShell>
  )
}

interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
  hint?: ReactNode
}

export function TextArea({ label, error, hint, id: idProp, className, required, ...rest }: TextAreaProps) {
  const auto = useId()
  const id = idProp ?? auto
  return (
    <FieldShell id={id} label={label} error={error} hint={hint} required={required}>
      <textarea
        id={id}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(control, 'min-h-28 resize-y', error ? 'border-error' : 'border-border', className)}
        {...rest}
      />
    </FieldShell>
  )
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  hint?: ReactNode
  options: { value: string; label: string }[]
}

export function SelectField({ label, error, hint, id: idProp, options, className, required, ...rest }: SelectProps) {
  const auto = useId()
  const id = idProp ?? auto
  return (
    <FieldShell id={id} label={label} error={error} hint={hint} required={required}>
      <select
        id={id}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(control, 'appearance-auto', error ? 'border-error' : 'border-border', className)}
        {...rest}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </FieldShell>
  )
}

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: ReactNode
  description?: ReactNode
  error?: string
}

export function Checkbox({ label, description, error, id: idProp, ...rest }: CheckboxProps) {
  const auto = useId()
  const id = idProp ?? auto
  return (
    <div>
      <div className="flex items-start gap-3">
        <input
          id={id}
          type="checkbox"
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : description ? `${id}-desc` : undefined}
          className="mt-0.5 size-5 shrink-0 accent-[var(--kk-primary)]"
          {...rest}
        />
        <label htmlFor={id} className="text-sm">
          <span className="font-medium">{label}</span>
          {description && (
            <span id={`${id}-desc`} className="mt-0.5 block text-muted">
              {description}
            </span>
          )}
        </label>
      </div>
      {error && (
        <p id={`${id}-error`} className="mt-1 text-sm text-error">
          {error}
        </p>
      )}
    </div>
  )
}
