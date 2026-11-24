import { QueryClient } from '@tanstack/react-query'
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister'
import { ApiError } from '@/api/client'

export const CACHE_KEY = 'kk.cache'
const DAY = 24 * 60 * 60 * 1000

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        gcTime: 7 * DAY, // kept so the persisted cache can serve pages offline
        retry: (count, error) => {
          if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false
          return count < 2
        },
        // Show cached data while offline instead of pausing with a spinner.
        networkMode: 'offlineFirst',
      },
      mutations: { retry: false },
    },
  })
}

export const persister = createSyncStoragePersister({
  storage: typeof window === 'undefined' ? undefined : window.localStorage,
  key: CACHE_KEY,
  throttleTime: 1500,
})

export const persistOptions = {
  persister,
  maxAge: 7 * DAY,
  buster: 'v1',
  dehydrateOptions: {
    // Persist successful reads only; never the assistant conversation contents or moderation queues.
    shouldDehydrateQuery: (q: { state: { status: string }; queryKey: readonly unknown[] }) =>
      q.state.status === 'success' && !['assistant', 'moderation'].includes(String(q.queryKey[0])),
  },
}
