import type { InfiniteData, QueryClient, QueryKey } from '@tanstack/react-query'
import type { Page } from '@/api/types'

type Cached<T> = T | Page<T> | InfiniteData<Page<T>> | undefined

/**
 * Applies `update` to every cached copy of an item (detail, plain page or infinite list) under `prefix`.
 * Used for optimistic bookmark/like toggles so every screen agrees immediately.
 */
export function patchEverywhere<T extends { id: string }>(qc: QueryClient, prefix: QueryKey, id: string, update: (item: T) => T) {
  const patchPage = (p: Page<T>): Page<T> => ({ ...p, results: p.results.map((it) => (it.id === id ? update(it) : it)) })
  qc.setQueriesData<Cached<T>>({ queryKey: prefix }, (input): Cached<T> => {
    const data = input as unknown
    if (!data || typeof data !== 'object') return input
    const rec = data as { pages?: unknown; results?: unknown; id?: unknown }
    if (Array.isArray(rec.pages)) {
      const inf = data as InfiniteData<Page<T>>
      return { ...inf, pages: inf.pages.map(patchPage) }
    }
    if (Array.isArray(rec.results)) return patchPage(data as Page<T>)
    if (rec.id === id) return update(data as T)
    return input
  })
}

export function flatten<T>(data?: InfiniteData<Page<T>>): T[] {
  return data?.pages.flatMap((p) => p.results) ?? []
}
