import { api, type Query } from './client'
import type {
  AlumniOwn,
  AlumniOwnInput,
  AlumniPublic,
  AuthResponse,
  ChatResponse,
  ChatTask,
  Comment,
  Contribution,
  ContributionType,
  Conversation,
  ConversationDetail,
  HelpArea,
  Language,
  LikeState,
  MentorshipRequest,
  MentorshipUpdateStatus,
  Notification,
  Opportunity,
  Page,
  Project,
  ProjectInput,
  RegisterRole,
  ReportReason,
  ReportTarget,
  Service,
  StoryDetail,
  StoryList,
  User,
} from './types'

export const authApi = {
  login: (body: { email: string; password: string }) => api<AuthResponse>('auth/login/', { method: 'POST', body }),
  register: (body: { email: string; password: string; full_name: string; role: RegisterRole; preferred_language: Language }) =>
    api<AuthResponse>('auth/register/', { method: 'POST', body }),
  logout: (refresh: string) => api<void>('auth/logout/', { method: 'POST', body: { refresh } }),
  me: () => api<User>('auth/me/'),
  updateMe: (body: { full_name?: string; preferred_language?: Language; profile?: { bio?: string; location?: string } }) =>
    api<User>('auth/me/', { method: 'PATCH', body }),
  uploadAvatar: (file: File) => {
    const fd = new FormData()
    fd.append('avatar', file)
    return api<User>('auth/me/avatar/', { method: 'POST', body: fd })
  },
  requestPasswordReset: (email: string) => api<void>('auth/password-reset/', { method: 'POST', body: { email } }),
  confirmPasswordReset: (body: { uid: string; token: string; new_password: string }) =>
    api<void>('auth/password-reset/confirm/', { method: 'POST', body }),
  changePassword: (body: { old_password: string; new_password: string }) =>
    api<void>('auth/change-password/', { method: 'POST', body }),
}

export const opportunitiesApi = {
  list: (query: Query) => api<Page<Opportunity>>('opportunities/', { query }),
  get: (id: string) => api<Opportunity>(`opportunities/${id}/`),
  bookmark: (id: string, on: boolean) => api<unknown>(`opportunities/${id}/bookmark/`, { method: on ? 'POST' : 'DELETE' }),
}

export const servicesApi = {
  list: (query: Query) => api<Page<Service>>('services/', { query }),
  get: (id: string) => api<Service>(`services/${id}/`),
}

export interface StoryInput {
  title: string
  body: string
  excerpt?: string
  category: string
  language: Language
  consent_to_publish: boolean
  cover?: File | null
}

function storyBody(input: Partial<StoryInput>): FormData | Record<string, unknown> {
  const { cover, ...rest } = input
  if (!cover) return rest
  const fd = new FormData()
  for (const [k, v] of Object.entries(rest)) if (v !== undefined) fd.append(k, String(v))
  fd.append('cover_image', cover)
  return fd
}

export const storiesApi = {
  list: (query: Query) => api<Page<StoryList>>('stories/', { query }),
  get: (id: string) => api<StoryDetail>(`stories/${id}/`),
  create: (input: StoryInput) => api<StoryDetail>('stories/', { method: 'POST', body: storyBody(input) }),
  update: (id: string, input: Partial<StoryInput>) => api<StoryDetail>(`stories/${id}/`, { method: 'PATCH', body: storyBody(input) }),
  remove: (id: string) => api<void>(`stories/${id}/`, { method: 'DELETE' }),
  publish: (id: string) => api<StoryDetail>(`stories/${id}/publish/`, { method: 'POST' }),
  like: (id: string, on: boolean) => api<LikeState>(`stories/${id}/like/`, { method: on ? 'POST' : 'DELETE' }),
  bookmark: (id: string, on: boolean) => api<unknown>(`stories/${id}/bookmark/`, { method: on ? 'POST' : 'DELETE' }),
  comments: (id: string, page: number) => api<Page<Comment>>(`stories/${id}/comments/`, { query: { page } }),
  addComment: (id: string, body: string) => api<Comment>(`stories/${id}/comments/`, { method: 'POST', body: { body } }),
  deleteComment: (id: string) => api<void>(`comments/${id}/`, { method: 'DELETE' }),
}

export const alumniApi = {
  list: (query: Query) => api<Page<AlumniPublic>>('alumni/', { query }),
  get: (id: string) => api<AlumniPublic>(`alumni/${id}/`),
  mine: () => api<AlumniOwn>('alumni/profile/'),
  save: (body: AlumniOwnInput, exists: boolean) =>
    api<AlumniOwn>('alumni/profile/', { method: exists ? 'PATCH' : 'PUT', body }),
  withdraw: () => api<void>('alumni/profile/', { method: 'DELETE' }),
}

export const mentorshipApi = {
  list: (query: Query) => api<Page<MentorshipRequest>>('mentorship/requests/', { query }),
  create: (body: { mentor_id: string; goal: string; help_areas: HelpArea[]; message: string }) =>
    api<MentorshipRequest>('mentorship/requests/', { method: 'POST', body }),
  update: (id: string, body: { status: MentorshipUpdateStatus; response_message?: string }) =>
    api<MentorshipRequest>(`mentorship/requests/${id}/`, { method: 'PATCH', body }),
}

export const projectsApi = {
  list: (query: Query) => api<Page<Project>>('projects/', { query }),
  get: (id: string) => api<Project>(`projects/${id}/`),
  create: (body: ProjectInput) => api<Project>('projects/', { method: 'POST', body }),
  contributions: (id: string) => api<Page<Contribution>>(`projects/${id}/contributions/`),
  pledge: (id: string, body: { type: ContributionType; description: string; amount?: string | null }) =>
    api<Contribution>(`projects/${id}/contributions/`, { method: 'POST', body }),
}

export const assistantApi = {
  chat: (body: {
    message: string
    conversation_id?: string | null
    language?: Language
    task: ChatTask
    target_id?: string | null
    target_language?: Language
  }) => api<ChatResponse>('ai/chat/', { method: 'POST', body }),
  conversations: () => api<Page<Conversation>>('ai/conversations/'),
  conversation: (id: string) => api<ConversationDetail>(`ai/conversations/${id}/`),
  deleteConversation: (id: string) => api<void>(`ai/conversations/${id}/`, { method: 'DELETE' }),
}

export const notificationsApi = {
  list: (query: Query) => api<Page<Notification>>('notifications/', { query }),
  unreadCount: () => api<{ count: number }>('notifications/unread-count/'),
  markRead: (id: string) => api<unknown>(`notifications/${id}/read/`, { method: 'POST' }),
  markAllRead: () => api<{ updated: number }>('notifications/read-all/', { method: 'POST' }),
}

export const moderationApi = {
  stories: () => api<Page<StoryDetail>>('moderation/stories/'),
  approveStory: (id: string) => api<StoryDetail>(`moderation/stories/${id}/approve/`, { method: 'POST' }),
  rejectStory: (id: string, reason: string) =>
    api<StoryDetail>(`moderation/stories/${id}/reject/`, { method: 'POST', body: { reason } }),
  projects: () => api<Page<Project>>('moderation/projects/'),
  decideProject: (id: string, approve: boolean, note: string) =>
    api<Project>(`moderation/projects/${id}/${approve ? 'approve' : 'reject'}/`, { method: 'POST', body: { note } }),
  report: (body: { target_type: ReportTarget; target_id: string; reason: ReportReason; details?: string }) =>
    api<unknown>('moderation/reports/', { method: 'POST', body }),
}
