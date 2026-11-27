import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/render'
import { API, errorBody, http, HttpResponse, server } from '@/test/server'
import { ForgotPasswordPage } from './PasswordResetPages'

const extra = [{ path: '/login', element: <p>login page</p> }]
const CODE_ERROR = 'That code is wrong or has expired. Check the latest email, or ask for a new code.'

function renderReset() {
  return renderPage(<ForgotPasswordPage />, { route: '/forgot-password', path: '/forgot-password', extraRoutes: extra })
}

async function requestCode() {
  await userEvent.type(screen.getByLabelText(/^Email/), ' Amina@Demo.Kakuma.test ')
  await userEvent.click(screen.getByRole('button', { name: 'Send code' }))
  expect(await screen.findByRole('heading', { name: 'Enter your code' })).toBeInTheDocument()
}

describe('ForgotPasswordPage', () => {
  it('resets the password with the emailed code', async () => {
    const bodies: Record<string, unknown> = {}
    server.use(
      http.post(`${API}/auth/password-reset/`, async ({ request }) => {
        bodies.request = await request.json()
        return HttpResponse.json({ detail: 'sent' }, { status: 202 })
      }),
      http.post(`${API}/auth/password-reset/verify/`, async ({ request }) => {
        bodies.verify = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
      http.post(`${API}/auth/password-reset/confirm/`, async ({ request }) => {
        bodies.confirm = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { router } = renderReset()
    await requestCode()
    expect(screen.getByText(/amina@demo\.kakuma\.test/i)).toBeInTheDocument()
    expect(screen.getByText(/New code in \d+ s/)).toBeInTheDocument()

    // Letters and extra digits are ignored: only 6 digits are kept.
    await userEvent.type(screen.getByLabelText(/^6-digit code/), '48a29134')
    expect(screen.getByLabelText(/^6-digit code/)).toHaveValue('482913')
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }))

    await userEvent.type(await screen.findByLabelText(/^New password/), 'An0ther-strong-pass')
    await userEvent.type(screen.getByLabelText(/^Confirm password/), 'An0ther-strong-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Update password' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(bodies).toEqual({
      request: { email: 'amina@demo.kakuma.test' },
      verify: { email: 'amina@demo.kakuma.test', code: '482913' },
      confirm: { email: 'amina@demo.kakuma.test', code: '482913', new_password: 'An0ther-strong-pass' },
    })
  })

  it('asks for all 6 digits before calling the API', async () => {
    server.use(http.post(`${API}/auth/password-reset/`, () => HttpResponse.json({}, { status: 202 })))
    renderReset()
    await requestCode()
    await userEvent.type(screen.getByLabelText(/^6-digit code/), '123')
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(screen.getByText('Enter the 6-digit code from the email')).toBeInTheDocument()
  })

  it('shows the server message for a wrong code', async () => {
    server.use(
      http.post(`${API}/auth/password-reset/`, () => HttpResponse.json({}, { status: 202 })),
      http.post(`${API}/auth/password-reset/verify/`, () =>
        HttpResponse.json(errorBody('validation_error', 'Invalid input.', { code: [CODE_ERROR] }), { status: 400 }),
      ),
    )
    renderReset()
    await requestCode()
    await userEvent.type(screen.getByLabelText(/^6-digit code/), '000000')
    await userEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(await screen.findByText(CODE_ERROR)).toBeInTheDocument()
  })
})
