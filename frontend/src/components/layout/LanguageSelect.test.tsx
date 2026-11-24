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
    await userEvent.selectOptions(screen.getByLabelText('Language'), 'sw')
    expect(await screen.findByText('Bado hakuna arifa')).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('sw')
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
    await userEvent.selectOptions(await screen.findByLabelText('Language'), 'de')
    expect(await screen.findByText('Noch keine Benachrichtigungen')).toBeInTheDocument()
    await vi.waitFor(() => expect(patched).toEqual({ preferred_language: 'de' }))
  })
})
