import { describe, expect, it } from 'vitest'
import { notificationTarget } from './NotificationsPage'

describe('notificationTarget', () => {
  it('links each notification to the screen it is about', () => {
    expect(notificationTarget({ type: 'story_comment', data: { story_id: 's1', comment_id: 'c1' } })).toBe('/stories/s1')
    expect(notificationTarget({ type: 'project_status', data: { project_id: 'p1' } })).toBe('/projects/p1')
    expect(notificationTarget({ type: 'mentorship_request', data: {} })).toBe('/mentorship')
    expect(notificationTarget({ type: 'mentorship_update', data: {} })).toBe('/mentorship?box=sent')
    expect(notificationTarget({ type: 'system', data: null })).toBeNull()
  })
})
