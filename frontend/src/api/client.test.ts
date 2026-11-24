import { describe, expect, it, vi } from 'vitest'
import { API, errorBody, http, HttpResponse, server } from '@/test/server'
import { api, ApiError, buildUrl, nextPage, onSessionExpired } from './client'
import { tokenStore } from './tokens'

describe('api client', () => {
  it('maps the error envelope to ApiError with field details', async () => {
    server.use(http.post(`${API}/auth/register/`, () => HttpResponse.json(errorBody('validation_error', 'Invalid input.', { email: ['Already registered.'] }), { status: 400 })))
    const err = await api('auth/register/', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect((err as ApiError).status).toBe(400)
    expect((err as ApiError).code).toBe('validation_error')
    expect((err as ApiError).field('email')).toBe('Already registered.')
  })

  it('reports a network failure as status 0', async () => {
    server.use(http.get(`${API}/opportunities/`, () => HttpResponse.error()))
    const err = (await api('opportunities/').catch((e: unknown) => e)) as ApiError
    expect(err.isNetwork).toBe(true)
    expect(err.code).toBe('network_error')
  })

  it('sends the bearer token and Accept-Language', async () => {
    tokenStore.set({ access: 'abc', refresh: 'r1' })
    let auth: string | null = null
    let lang: string | null = null
    server.use(
      http.get(`${API}/auth/me/`, ({ request }) => {
        auth = request.headers.get('Authorization')
        lang = request.headers.get('Accept-Language')
        return HttpResponse.json({ id: 'u' })
      }),
    )
    await api('auth/me/')
    expect(auth).toBe('Bearer abc')
    expect(lang).toBe('en')
  })

  it('refreshes once for concurrent 401s and retries with the rotated token', async () => {
    tokenStore.set({ access: 'expired', refresh: 'r1' })
    const refreshBodies: unknown[] = []
    server.use(
      http.post(`${API}/auth/token/refresh/`, async ({ request }) => {
        refreshBodies.push(await request.json())
        return HttpResponse.json({ access: 'fresh', refresh: 'r2' })
      }),
      http.get(`${API}/notifications/unread-count/`, ({ request }) =>
        request.headers.get('Authorization') === 'Bearer fresh'
          ? HttpResponse.json({ count: 2 })
          : HttpResponse.json(errorBody('token_not_valid', 'Token expired'), { status: 401 }),
      ),
    )
    const results = await Promise.all([api<{ count: number }>('notifications/unread-count/'), api<{ count: number }>('notifications/unread-count/')])
    expect(results.map((r) => r.count)).toEqual([2, 2])
    expect(refreshBodies).toEqual([{ refresh: 'r1' }]) // single flight: rotated tokens can't be reused
    expect(tokenStore.refresh).toBe('r2')
  })

  it('expires the session when the refresh token is rejected', async () => {
    tokenStore.set({ access: 'expired', refresh: 'old' })
    const expired = vi.fn()
    const off = onSessionExpired(expired)
    server.use(
      http.post(`${API}/auth/token/refresh/`, () => HttpResponse.json(errorBody('token_not_valid', 'Token is blacklisted'), { status: 401 })),
      http.get(`${API}/notifications/`, () => HttpResponse.json(errorBody('not_authenticated', 'Login required'), { status: 401 })),
    )
    await expect(api('notifications/')).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledTimes(1)
    expect(tokenStore.hasSession()).toBe(false)
    off()
  })

  it('keeps the session when the refresh endpoint is down (5xx)', async () => {
    tokenStore.set({ access: 'expired', refresh: 'keep-me' })
    const expired = vi.fn()
    const off = onSessionExpired(expired)
    server.use(
      http.post(`${API}/auth/token/refresh/`, () => HttpResponse.json(errorBody('server_error', 'Down'), { status: 503 })),
      http.get(`${API}/notifications/`, () => HttpResponse.json(errorBody('token_not_valid', 'expired'), { status: 401 })),
    )
    await expect(api('notifications/')).rejects.toMatchObject({ status: 503 })
    expect(expired).not.toHaveBeenCalled()
    expect(tokenStore.refresh).toBe('keep-me')
    off()
  })

  it('returns undefined for 204 responses', async () => {
    server.use(http.delete(`${API}/stories/s1/`, () => new HttpResponse(null, { status: 204 })))
    await expect(api('stories/s1/', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('builds query strings without empty values', () => {
    expect(buildUrl('opportunities/', { search: 'tech', category: '', open: true, page: undefined })).toBe(`${API}/opportunities/?search=tech&open=true`)
  })

  it('reads the next page number from DRF links', () => {
    expect(nextPage('http://x/api/v1/stories/?page=3&page_size=9')).toBe(3)
    expect(nextPage(null)).toBeUndefined()
  })
})
