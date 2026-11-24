import { QueryClient, type InfiniteData } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import type { Opportunity, Page } from '@/api/types'
import { opportunity, page } from '@/test/server'
import { patchEverywhere } from './cache'

describe('patchEverywhere', () => {
  it('updates the same item in infinite lists, pages and detail caches', () => {
    const qc = new QueryClient()
    const item = opportunity({ id: 'a' })
    const other = opportunity({ id: 'b' })
    qc.setQueryData<InfiniteData<Page<Opportunity>>>(['opportunities', 'list', {}], { pages: [page([item, other])], pageParams: [1] })
    qc.setQueryData<Page<Opportunity>>(['opportunities', 'landing'], page([item]))
    qc.setQueryData<Opportunity>(['opportunities', 'detail', 'a'], item)

    patchEverywhere<Opportunity>(qc, ['opportunities'], 'a', (o) => ({ ...o, is_bookmarked: true }))

    const list = qc.getQueryData<InfiniteData<Page<Opportunity>>>(['opportunities', 'list', {}])!
    expect(list.pages[0].results.map((o) => o.is_bookmarked)).toEqual([true, false])
    expect(qc.getQueryData<Page<Opportunity>>(['opportunities', 'landing'])!.results[0].is_bookmarked).toBe(true)
    expect(qc.getQueryData<Opportunity>(['opportunities', 'detail', 'a'])!.is_bookmarked).toBe(true)
  })
})
