import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/render'
import { API, http, HttpResponse, page, server, user } from '@/test/server'
import AssistantPage from './AssistantPage'

describe('AssistantPage', () => {
  it('New chat clears a chat whose first reply is still on its way, and the late reply does not pull the user back', async () => {
    let releaseReply!: () => void
    const replyReady = new Promise<void>((resolve) => (releaseReply = resolve))
    server.use(
      http.get(`${API}/ai/conversations/`, () => HttpResponse.json(page([]))),
      http.get(`${API}/ai/conversations/c-old/`, () =>
        HttpResponse.json({ id: 'c-old', title: 'Old question', created_at: '', updated_at: '', messages: [] }),
      ),
      http.post(`${API}/ai/chat/`, async () => {
        await replyReady
        return HttpResponse.json({
          conversation_id: 'c-old',
          message: { id: 'm1', role: 'assistant', content: 'Old answer', sources: [], created_at: '' },
          sources: [],
        })
      }),
    )
    const { router } = renderPage(<AssistantPage />, { route: '/assistant', path: '/assistant/:conversationId?', user: user() })

    await userEvent.type(await screen.findByLabelText('Type your question…'), 'Old question')
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))
    expect(await screen.findByText('Thinking…')).toBeInTheDocument()

    await userEvent.click(screen.getAllByRole('button', { name: 'New chat' })[0])
    expect(screen.queryByText('Old question')).not.toBeInTheDocument()
    expect(screen.queryByText('Thinking…')).not.toBeInTheDocument()

    releaseReply()
    // The reply is saved to history on the server, but the user stays in their new chat.
    await waitFor(() => expect(router.state.location.pathname).toBe('/assistant'))
    await new Promise((r) => setTimeout(r, 50))
    expect(router.state.location.pathname).toBe('/assistant')
    expect(screen.queryByText('Old answer')).not.toBeInTheDocument()
  })
})
