import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/render'
import { API, errorBody, http, HttpResponse, opportunity, page, server, user } from '@/test/server'
import OpportunitiesPage from './OpportunitiesPage'

const list = [opportunity({ id: 'a', title: 'Undergraduate scholarship' }), opportunity({ id: 'b', title: 'Digital skills fellowship', category: 'vocational', is_remote: true })]

describe('OpportunitiesPage', () => {
  it('lists opportunities and sends filters from the URL to the API', async () => {
    const seen: string[] = []
    server.use(
      http.get(`${API}/opportunities/`, ({ request }) => {
        seen.push(new URL(request.url).search)
        return HttpResponse.json(page(list))
      }),
    )
    renderPage(<OpportunitiesPage />, { route: '/opportunities?category=scholarship', path: '/opportunities' })
    expect(await screen.findByRole('link', { name: 'Undergraduate scholarship' })).toHaveAttribute('href', '/opportunities/a')
    expect(screen.getByText('2 results')).toBeInTheDocument()
    expect(seen[0]).toContain('category=scholarship')
    expect(seen[0]).toContain('open=true')
  })

  it('shows an empty state with a way to clear filters', async () => {
    server.use(http.get(`${API}/opportunities/`, () => HttpResponse.json(page([]))))
    renderPage(<OpportunitiesPage />, { route: '/opportunities?search=zzz', path: '/opportunities' })
    expect(await screen.findByText('No opportunities match')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Clear filters' }).length).toBeGreaterThan(0)
  })

  it('shows an error with retry when the API fails', async () => {
    let calls = 0
    server.use(
      http.get(`${API}/opportunities/`, () => {
        calls += 1
        return calls === 1 ? HttpResponse.json(errorBody('server_error', 'boom'), { status: 500 }) : HttpResponse.json(page(list))
      }),
    )
    renderPage(<OpportunitiesPage />, { route: '/opportunities', path: '/opportunities' })
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on our side')
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Digital skills fellowship')).toBeInTheDocument()
  })

  it('sends guests to sign in when they try to bookmark', async () => {
    server.use(http.get(`${API}/opportunities/`, () => HttpResponse.json(page(list))))
    const { router } = renderPage(<OpportunitiesPage />, { route: '/opportunities', path: '/opportunities' })
    const card = (await screen.findByText('Undergraduate scholarship')).closest('article')!
    await userEvent.click(within(card).getByRole('button', { name: 'Save opportunity' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe('?next=%2Fopportunities')
  })

  it('bookmarks optimistically for members and confirms with a short toast', async () => {
    let bookmarked = false
    server.use(
      http.get(`${API}/opportunities/`, () => HttpResponse.json(page(list.map((o) => (o.id === 'a' ? { ...o, is_bookmarked: bookmarked } : o))))),
      http.post(`${API}/opportunities/a/bookmark/`, () => {
        bookmarked = true
        return HttpResponse.json({ bookmarked: true }, { status: 201 })
      }),
    )
    renderPage(<OpportunitiesPage />, { route: '/opportunities', path: '/opportunities', user: user() })
    const card = (await screen.findByText('Undergraduate scholarship')).closest('article')!
    await userEvent.click(within(card).getByRole('button', { name: 'Save opportunity' }))
    expect(await within(card).findByRole('button', { name: 'Remove from saved' })).toHaveAttribute('aria-pressed', 'true')
    expect(await screen.findByText('Saved')).toBeInTheDocument()
  })

  it('rolls the bookmark back when the server refuses it', async () => {
    server.use(
      http.get(`${API}/opportunities/`, () => HttpResponse.json(page(list))),
      http.post(`${API}/opportunities/a/bookmark/`, () => HttpResponse.json(errorBody('not_found', 'Gone'), { status: 404 })),
    )
    renderPage(<OpportunitiesPage />, { route: '/opportunities', path: '/opportunities', user: user() })
    const card = (await screen.findByText('Undergraduate scholarship')).closest('article')!
    await userEvent.click(within(card).getByRole('button', { name: 'Save opportunity' }))
    expect(await screen.findByText("We couldn't find that. It may have been removed.")).toBeInTheDocument()
    expect(within(card).getByRole('button', { name: 'Save opportunity' })).toHaveAttribute('aria-pressed', 'false')
  })
})
