import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { tokenStore } from '@/api/tokens'
import { renderPage } from '@/test/render'
import { API, errorBody, http, HttpResponse, server, user } from '@/test/server'
import LoginPage from './LoginPage'

const extra = [{ path: '/opportunities', element: <p>opportunities page</p> }]

describe('LoginPage', () => {
  it('shows inline errors without calling the API', async () => {
    renderPage(<LoginPage />, { route: '/sign-in', path: '/sign-in' })
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getAllByText('This field is required.')).toHaveLength(2)
    expect(screen.getByLabelText(/^Email/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows a clear message for wrong credentials', async () => {
    server.use(http.post(`${API}/auth/login/`, () => HttpResponse.json(errorBody('authentication_failed', 'No active account'), { status: 401 })))
    renderPage(<LoginPage />, { route: '/sign-in', path: '/sign-in' })
    await userEvent.type(screen.getByLabelText(/^Email/), 'amina@demo.kakuma.test')
    await userEvent.type(screen.getByLabelText(/^Password/), 'wrong-password')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect.')
    expect(tokenStore.hasSession()).toBe(false)
  })

  it('signs in, stores the session, toasts and returns to the requested page', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/login/`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ user: user(), tokens: { access: 'a1', refresh: 'r1' } })
      }),
    )
    const { router } = renderPage(<LoginPage />, { route: '/sign-in?next=%2Fopportunities', path: '/sign-in', extraRoutes: extra })
    await userEvent.type(screen.getByLabelText(/^Email/), '  Amina@Demo.Kakuma.test ')
    await userEvent.type(screen.getByLabelText(/^Password/), 'kakuma-2026')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('opportunities page')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/opportunities')
    expect(body).toEqual({ email: 'amina@demo.kakuma.test', password: 'kakuma-2026' })
    expect(tokenStore.refresh).toBe('r1')
    expect(screen.getByText('Welcome back, Amina')).toBeInTheDocument()
  })

  it('ignores an off-site redirect target', async () => {
    server.use(http.post(`${API}/auth/login/`, () => HttpResponse.json({ user: user(), tokens: { access: 'a', refresh: 'r' } })))
    const { router } = renderPage(<LoginPage />, {
      route: '/sign-in?next=%2F%2Fevil.example',
      path: '/sign-in',
      extraRoutes: [{ path: '/', element: <p>home</p> }],
    })
    await userEvent.type(screen.getByLabelText(/^Email/), 'amina@demo.kakuma.test')
    await userEvent.type(screen.getByLabelText(/^Password/), 'kakuma-2026')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })
})
