import type { ReactElement } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, Outlet, RouterProvider, type RouteObject } from 'react-router'
import { tokenStore } from '@/api/tokens'
import type { User } from '@/api/types'
import { ToastProvider } from '@/components/ui/Toast'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { API, http, HttpResponse, server } from './server'

interface Options {
  route?: string
  path?: string
  /** Signed-in user: seeds tokens and the cached profile the AuthProvider restores from. */
  user?: User
  extraRoutes?: RouteObject[]
}

export function renderPage(ui: ReactElement, { route = '/', path = '/', user, extraRoutes = [] }: Options = {}) {
  if (user) {
    tokenStore.set({ access: 'access-token', refresh: 'refresh-token' })
    localStorage.setItem('kk.user', JSON.stringify(user))
    server.use(http.get(`${API}/auth/me/`, () => HttpResponse.json(user)))
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } } })
  const router = createMemoryRouter(
    [
      {
        element: (
          <ToastProvider>
            <AuthProvider>
              <Outlet />
            </AuthProvider>
          </ToastProvider>
        ),
        children: [
          { path, element: ui },
          { path: '/login', element: <p>login page</p> },
          ...extraRoutes,
        ],
      },
    ],
    { initialEntries: [route] },
  )
  return { router, queryClient, ...render(<QueryClientProvider client={queryClient}><RouterProvider router={router} /></QueryClientProvider>) }
}
