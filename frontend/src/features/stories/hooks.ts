import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { nextPage, type Query } from '@/api/client'
import { storiesApi } from '@/api/endpoints'
import type { StoryCategory, StoryList } from '@/api/types'
import { useErrorMessage } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast-context'
import { patchEverywhere } from '@/lib/cache'

export const STORY_CATEGORIES: StoryCategory[] = ['education', 'career', 'resettlement', 'entrepreneurship', 'inspiration', 'advice', 'community']

export function useStories(filters: Query, pageSize = 9) {
  return useInfiniteQuery({
    queryKey: ['stories', 'list', filters],
    queryFn: ({ pageParam }) => storiesApi.list({ ...filters, page: pageParam, page_size: pageSize }),
    initialPageParam: 1,
    getNextPageParam: (last) => nextPage(last.next),
  })
}

export function useStory(id: string, enabled = true) {
  return useQuery({ queryKey: ['stories', 'detail', id], queryFn: () => storiesApi.get(id), enabled: enabled && Boolean(id) })
}

export function useToggleLike() {
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  return useMutation({
    mutationFn: ({ id, on }: { id: string; on: boolean }) => storiesApi.like(id, on),
    onMutate: ({ id, on }) =>
      patchEverywhere<StoryList>(qc, ['stories'], id, (s) => ({ ...s, is_liked: on, likes_count: Math.max(0, s.likes_count + (on ? 1 : -1)) })),
    onSuccess: (res, { id }) => patchEverywhere<StoryList>(qc, ['stories'], id, (s) => ({ ...s, is_liked: res.liked, likes_count: res.likes_count })),
    onError: (err, { id, on }) => {
      patchEverywhere<StoryList>(qc, ['stories'], id, (s) => ({ ...s, is_liked: !on, likes_count: Math.max(0, s.likes_count + (on ? -1 : 1)) }))
      toast.error(errorMessage(err))
    },
  })
}

export function useToggleStoryBookmark() {
  const qc = useQueryClient()
  const toast = useToast()
  const { t } = useTranslation()
  const errorMessage = useErrorMessage()
  return useMutation({
    mutationFn: ({ id, on }: { id: string; on: boolean }) => storiesApi.bookmark(id, on),
    onMutate: ({ id, on }) => patchEverywhere<StoryList>(qc, ['stories'], id, (s) => ({ ...s, is_bookmarked: on })),
    onSuccess: (_r, { on }) => toast.success(on ? t('stories.saved') : t('stories.removed')),
    onError: (err, { id, on }) => {
      patchEverywhere<StoryList>(qc, ['stories'], id, (s) => ({ ...s, is_bookmarked: !on }))
      toast.error(errorMessage(err))
    },
  })
}
