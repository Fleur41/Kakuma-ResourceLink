import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderPage } from '@/test/render'
import { API, http, HttpResponse, server, user } from '@/test/server'
import { EmptyState } from '@/components/ui/feedback'
import { useTranslation } from 'react-i18next'
import { LanguageSelect } from './LanguageSelect'

function Probe() {
  const { t } = useTranslation()
  return (
    <>
      <LanguageSelect />
      <EmptyState title={t('notifications.empty')} />
    </>
  )
}

describe('LanguageSelect', () => {
  it('switches the UI language and updates <html lang>', async () => {
    renderPage(<Probe />)
    await userEvent.click(screen.getByRole('button', { name: /Language/ }))
    expect(screen.getByRole('option', { name: 'English' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(screen.getByRole('option', { name: 'Kiswahili' }))
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(await screen.findByText('Bado hakuna arifa')).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('sw')
  })

  it('opens with the keyboard and closes on Escape', async () => {
    renderPage(<Probe />)
    const button = screen.getByRole('button', { name: /Language/ })
    button.focus()
    await userEvent.keyboard('{ArrowDown}')
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['English', 'Kiswahili', 'Español', 'Italiano', 'Deutsch'])
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(button).toHaveFocus()
  })

  it("saves a signed-in member's choice to their profile", async () => {
    let patched: unknown
    server.use(
      http.patch(`${API}/auth/me/`, async ({ request }) => {
        patched = await request.json()
        return HttpResponse.json(user({ preferred_language: 'de' }))
      }),
    )
    renderPage(<Probe />, { user: user() })
    await userEvent.click(await screen.findByRole('button', { name: /Language/ }))
    await userEvent.keyboard('{End}{Enter}') // Deutsch is last
    expect(await screen.findByText('Noch keine Benachrichtigungen')).toBeInTheDocument()
    await vi.waitFor(() => expect(patched).toEqual({ preferred_language: 'de' }))
  })
})
