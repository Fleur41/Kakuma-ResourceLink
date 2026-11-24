import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/render'
import { API, http, HttpResponse, page, server, story, user } from '@/test/server'
import StoryPage from './StoryPage'

function mockStory(overrides = {}) {
  server.use(
    http.get(`${API}/stories/st-1/`, () => HttpResponse.json(story(overrides))),
    http.get(`${API}/stories/st-1/comments/`, () => HttpResponse.json(page([]))),
  )
}

describe('StoryPage', () => {
  it('renders Markdown without raw HTML or javascript: links', async () => {
    mockStory({ body: '## Journey\n\n<script>alert(1)</script><img src=x onerror=alert(1)>\n\n[click](javascript:alert(1)) and [site](https://example.org)' })
    const { container } = renderPage(<StoryPage />, { route: '/stories/st-1', path: '/stories/:id' })
    expect(await screen.findByRole('heading', { name: 'Journey' })).toBeInTheDocument()
    expect(container.querySelector('script')).toBeNull()
    expect(container.querySelector('img[onerror]')).toBeNull()
    expect(screen.getByText('click').getAttribute('href') ?? '').not.toContain('javascript')
    expect(screen.getByRole('link', { name: 'site' })).toHaveAttribute('rel', expect.stringContaining('noopener'))
  })

  it('shows the moderation status and feedback to the author of a rejected story', async () => {
    mockStory({ status: 'rejected', rejection_reason: 'Please remove the phone number.', author: { id: 'u-1', full_name: 'Amina Yusuf', avatar: null, country: null } })
    renderPage(<StoryPage />, { route: '/stories/st-1', path: '/stories/:id', user: user() })
    expect(await screen.findByText('Rejected')).toBeInTheDocument()
    expect(screen.getByText('Please remove the phone number.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/stories/st-1/edit')
    expect(screen.queryByRole('button', { name: /like/i })).not.toBeInTheDocument()
  })

  it('invites guests to sign in to comment', async () => {
    mockStory()
    renderPage(<StoryPage />, { route: '/stories/st-1', path: '/stories/:id' })
    expect(await screen.findByText('to join the conversation.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '3 likes' })).toBeInTheDocument()
  })
})
