import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/render'
import { user } from '@/test/server'
import { RequireAuth } from './guards'

describe('RequireAuth', () => {
  it('redirects guests to sign in with a return path', async () => {
    const { router } = renderPage(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
      { route: '/mentorship?box=sent', path: '/mentorship' },
    )
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe(`?next=${encodeURIComponent('/mentorship?box=sent')}`)
    expect(screen.queryByText('secret')).not.toBeInTheDocument()
  })

  it('renders member pages for signed-in users', async () => {
    renderPage(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
      { route: '/mentorship', path: '/mentorship', user: user() },
    )
    expect(await screen.findByText('secret')).toBeInTheDocument()
  })

  it('keeps members out of the moderation queue', async () => {
    const { router } = renderPage(
      <RequireAuth moderator>
        <p>queue</p>
      </RequireAuth>,
      { route: '/moderation', path: '/moderation', user: user(), extraRoutes: [{ path: '/', element: <p>home</p> }] },
    )
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })
})
