/**
 * Token storage. The access token (15 min) lives only in memory. The refresh token (7 days, rotated on
 * every use) is kept in localStorage so a reload keeps the session; it is cleared on logout or when the
 * server rejects it. XSS exposure is limited by rendering user Markdown without raw HTML.
 */
const REFRESH_KEY = 'kk.refresh'

let access: string | null = null

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key: string, value: string | null) {
  try {
    if (value === null) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    /* storage unavailable (private mode): session lasts for this tab only */
  }
}

export const tokenStore = {
  get access() {
    return access
  },
  get refresh() {
    return safeGet(REFRESH_KEY)
  },
  set(tokens: { access: string; refresh?: string }) {
    access = tokens.access
    if (tokens.refresh) safeSet(REFRESH_KEY, tokens.refresh)
  },
  clear() {
    access = null
    safeSet(REFRESH_KEY, null)
  },
  hasSession() {
    return access !== null || safeGet(REFRESH_KEY) !== null
  },
}
