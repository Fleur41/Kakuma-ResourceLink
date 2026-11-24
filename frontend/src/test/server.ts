import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { Opportunity, Page, StoryDetail, User } from '@/api/types'

export const API = 'http://api.test/api/v1'
export const server = setupServer()
export { http, HttpResponse }

export function page<T>(results: T[], next: string | null = null): Page<T> {
  return { count: results.length, next, previous: null, results }
}

export function user(overrides: Partial<User> = {}): User {
  return {
    id: 'u-1',
    email: 'amina@demo.kakuma.test',
    full_name: 'Amina Yusuf',
    role: 'member',
    preferred_language: 'en',
    date_joined: '2026-01-10T10:00:00Z',
    profile: { bio: '', location: 'Kakuma', avatar: null },
    has_alumni_profile: false,
    ...overrides,
  }
}

export function opportunity(overrides: Partial<Opportunity> = {}): Opportunity {
  return {
    id: 'op-1',
    title: 'Undergraduate scholarship',
    category: 'scholarship',
    organization: 'Education partner',
    summary: 'Tuition support for first-year students.',
    description: 'Full description',
    requirements: ['Secondary school certificate'],
    location: 'Kakuma',
    is_remote: false,
    deadline: null,
    application_url: 'https://example.org/apply',
    language: 'en',
    is_verified: false,
    is_bookmarked: false,
    created_at: '2026-11-01T10:00:00Z',
    updated_at: '2026-11-01T10:00:00Z',
    ...overrides,
  }
}

export function story(overrides: Partial<StoryDetail> = {}): StoryDetail {
  return {
    id: 'st-1',
    title: 'From Kakuma to a career in tech',
    slug: 'from-kakuma',
    excerpt: 'The first step was believing.',
    cover_image: null,
    category: 'career',
    language: 'en',
    status: 'published',
    author: { id: 'u-2', full_name: 'Daniel M.', avatar: null, country: 'Germany' },
    reading_time: 6,
    likes_count: 3,
    comments_count: 0,
    is_liked: false,
    is_bookmarked: false,
    published_at: '2026-11-20T10:00:00Z',
    created_at: '2026-11-19T10:00:00Z',
    updated_at: '2026-11-20T10:00:00Z',
    body: '# Hello\n\nA story.',
    consent_to_publish: true,
    rejection_reason: '',
    ...overrides,
  }
}

export const errorBody = (code: string, message: string, details: Record<string, string[]> = {}) => ({ error: { code, message, details } })
