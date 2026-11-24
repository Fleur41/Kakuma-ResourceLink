import type { components } from './schema'

type S = components['schemas']

/** The schema marks image fields as strings, but the API returns null when no file is set. */
type WithNullableImages<T> = {
  [K in keyof T]: K extends 'avatar' | 'cover_image'
    ? string | null
    : NonNullable<T[K]> extends unknown[]
      ? T[K]
      : NonNullable<T[K]> extends object
        ? WithNullableImages<NonNullable<T[K]>> | Extract<T[K], undefined>
        : T[K]
}

export type Language = S['LanguageEnum']
export type User = WithNullableImages<S['User']>
export type UserRole = S['UserRoleEnum']
export type RegisterRole = S['RegisterRoleEnum']
export type TokenPair = S['TokenPair']
export type AuthResponse = { user: User; tokens: TokenPair }
export type PublicUser = WithNullableImages<S['PublicUser']>

export type Opportunity = S['Opportunity']
export type OpportunityCategory = S['OpportunityCategoryEnum']
export type Service = S['Service']
export type ServiceCategory = S['ServiceCategoryEnum']

export type StoryList = WithNullableImages<S['StoryList']>
export type StoryDetail = WithNullableImages<S['StoryDetail']>
export type StoryCategory = S['StoryCategoryEnum']
export type StoryStatus = S['StoryStatusEnum']
export type Comment = WithNullableImages<S['Comment']>
export type LikeState = S['LikeState']

export type AlumniPublic = Omit<WithNullableImages<S['AlumniPublic']>, 'skills' | 'mentorship_areas' | 'contribution_interests'> & {
  skills: string[]
  mentorship_areas: string[]
  contribution_interests: string[]
}
export type AlumniOwn = S['AlumniOwn']
export type AlumniOwnInput = S['AlumniOwnRequest']
export type AlumniVisibility = S['AlumniVisibilityEnum']

export type MentorshipRequest = WithNullableImages<S['MentorshipRequest']>
export type MentorshipStatus = S['MentorshipStatusEnum']
export type MentorshipUpdateStatus = S['MentorshipStatusUpdateStatusEnum']
export type HelpArea = S['HelpAreaEnum']

export type Project = WithNullableImages<S['CommunityProject']>
export type ProjectInput = S['CommunityProjectRequest']
export type ProjectCategory = S['ProjectCategoryEnum']
export type ProjectStatus = S['ProjectStatusEnum']
export type Contribution = WithNullableImages<S['Contribution']>
export type ContributionType = S['ContributionTypeEnum']

export type ChatTask = S['TaskEnum']
export type ChatResponse = S['ChatResponse']
export type AIMessage = S['AIMessage']
export type AISource = S['Source']
export type Conversation = S['Conversation']
export type ConversationDetail = S['ConversationDetail']

export type Notification = S['Notification']
export type NotificationType = S['NotificationTypeEnum']

export type ReportReason = S['ReportReasonEnum']
export type ReportTarget = S['ReportTargetTypeEnum']

export interface Page<T> {
  count: number
  next?: string | null
  previous?: string | null
  results: T[]
}
