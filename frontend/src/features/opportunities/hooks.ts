import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { nextPage, type Query } from '@/api/client'
import { opportunitiesApi } from '@/api/endpoints'
import type { Opportunity } from '@/api/types'
import { useErrorMessage } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast-context'
import { patchEverywhere } from '@/lib/cache'

export function useOpportunities(filters: Query) {
  return useInfiniteQuery({
    queryKey: ['opportunities', 'list', filters],
    queryFn: ({ pageParam }) => opportunitiesApi.list({ ...filters, page: pageParam, page_size: 12 }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
}

export function useOpportunity(id: string) {
  return useQuery({ queryKey: ['opportunities', 'detail', id], queryFn: () => opportunitiesApi.get(id) })
}

/** Optimistic bookmark with rollback and a short toast. */
export function useToggleOpportunityBookmark() {
  const qc = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
  const errorMessage = useErrorMessage()
  return useMutation({
    mutationFn: ({ id, on }: { id: string; on: boolean }) => opportunitiesApi.bookmark(id, on),
    onMutate: async ({ id, on }) => {
      await qc.cancelQueries({ queryKey: ['opportunities'] })
      patchEverywhere<Opportunity>(qc, ['opportunities'], id, (o) => ({ ...o, is_bookmarked: on }))
    },
    onSuccess: (_d, { on }) => toast.success(on ? t('opportunities.saved') : t('opportunities.removed')),
    onError: (err, { id, on }) => {
      patchEverywhere<Opportunity>(qc, ['opportunities'], id, (o) => ({ ...o, is_bookmarked: !on }))
      toast.error(errorMessage(err))
    },
    onSettled: (_d, _e, { on }) => {
      if (!on) void qc.invalidateQueries({ queryKey: ['opportunities', 'list'], predicate: (q) => JSON.stringify(q.queryKey).includes('"bookmarked":true') })
    },
  })
}
