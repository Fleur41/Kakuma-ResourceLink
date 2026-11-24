import { tokenStore } from './tokens'

export const API_BASE: string = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? '/api/v1'

/** Error shape for every failed call. `status` 0 means the request never reached the server. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: Record<string, string[]>

  constructor(status: number, code: string, message: string, details: Record<string, string[]> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  get isNetwork() {
    return this.status === 0
  }

  /** First message for a field, for inline form errors. */
  field(name: string): string | undefined {
    const v = this.details[name]
    return Array.isArray(v) ? v[0] : typeof v === 'string' ? v : undefined
  }
}

type Listener = () => void
const sessionExpiredListeners = new Set<Listener>()

/** Called when the refresh token is rejected: the user must sign in again. */
export function onSessionExpired(fn: Listener): () => void {
  sessionExpiredListeners.add(fn)
  return () => sessionExpiredListeners.delete(fn)
}

let languageGetter: () => string = () => 'en'
export function setLanguageGetter(fn: () => string) {
  languageGetter = fn
}

export type Query = Record<string, string | number | boolean | undefined | null>

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  query?: Query
  body?: unknown
  signal?: AbortSignal
}

export function buildUrl(path: string, query?: Query): string {
  const url = `${API_BASE}/${path.replace(/^\//, '')}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v === undefined || v === null || v === '') continue
    params.set(k, String(v))
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

async function parseError(res: Response): Promise<ApiError> {
  let payload: unknown = null
  try {
    payload = await res.json()
  } catch {
    /* non-JSON error body */
  }
  const err = (payload as { error?: { code?: string; message?: string; details?: Record<string, string[]> } } | null)?.error
  return new ApiError(res.status, err?.code ?? 'server_error', err?.message ?? res.statusText ?? 'Request failed', err?.details ?? {})
}

let refreshInFlight: Promise<boolean> | null = null

/**
 * Exchanges the refresh token for a new pair. Only one refresh runs at a time: concurrent 401s wait for
 * the same promise, because refresh tokens rotate and the old one is blacklisted after use.
 * Returns false when there is no session. A network/5xx failure throws and keeps the session.
 */
export function refreshTokens(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight
  const refresh = tokenStore.refresh
  if (!refresh) return Promise.resolve(false)

  refreshInFlight = (async () => {
    let res: Response
    try {
      res = await fetch(buildUrl('auth/token/refresh/'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ refresh }),
      })
    } catch {
      throw new ApiError(0, 'network_error', 'Network unavailable')
    }
    if (res.ok) {
      const data = (await res.json()) as { access: string; refresh?: string }
      tokenStore.set(data)
      return true
    }
    if (res.status === 401 || res.status === 400) {
      // Another tab may have rotated the token a moment ago; only expire if it is still the one we sent.
      if (tokenStore.refresh === refresh) {
        tokenStore.clear()
        sessionExpiredListeners.forEach((l) => l())
      }
      return false
    }
    throw await parseError(res)
  })().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

async function send(path: string, opts: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Accept-Language': languageGetter(),
  }
  let body: BodyInit | undefined
  if (opts.body instanceof FormData) {
    body = opts.body
  } else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(opts.body)
  }
  if (tokenStore.access) headers.Authorization = `Bearer ${tokenStore.access}`
  try {
    return await fetch(buildUrl(path, opts.query), { method: opts.method ?? 'GET', headers, body, signal: opts.signal })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, 'network_error', 'Network unavailable')
  }
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  // After a reload only the refresh token survives; get an access token before the first call.
  if (!tokenStore.access && tokenStore.refresh) {
    try {
      await refreshTokens()
    } catch {
      /* offline: continue anonymously, cached data still shows */
    }
  }

  let res = await send(path, opts)
  if (res.status === 401 && tokenStore.refresh) {
    const refreshed = await refreshTokens()
    // Retry once with the new token, or anonymously if the session just expired (public endpoints still work).
    res = await send(path, opts)
    if (!refreshed && res.status === 401) throw await parseError(res)
  }
  if (!res.ok) throw await parseError(res)
  if (res.status === 204 || res.headers.get('Content-Length') === '0') return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Page number from a DRF `next` link, for infinite queries. */
export function nextPage(next?: string | null): number | undefined {
  if (!next) return undefined
  try {
    const p = new URL(next, 'http://x').searchParams.get('page')
    return p ? Number(p) : undefined
  } catch {
    return undefined
  }
}
