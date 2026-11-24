import { lazy } from 'react'
import type { RouteObject } from 'react-router'
import { AppShell } from '@/components/layout/AppShell'
import { GuestOnly, RequireAuth } from '@/features/auth/guards'
import HomePage from '@/features/home/HomePage'
import Root from './Root'
import RouteError from './RouteError'

// Route-level code splitting keeps the first load small on slow connections.
const LoginPage = lazy(() => import('@/features/auth/LoginPage'))
const RegisterPage = lazy(() => import('@/features/auth/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('@/features/auth/PasswordResetPages').then((m) => ({ default: m.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('@/features/auth/PasswordResetPages').then((m) => ({ default: m.ResetPasswordPage })))
const OpportunitiesPage = lazy(() => import('@/features/opportunities/OpportunitiesPage'))
const OpportunityDetailPage = lazy(() => import('@/features/opportunities/OpportunityDetailPage'))
const ServicesPage = lazy(() => import('@/features/services/ServicesPage'))
const ServiceDetailPage = lazy(() => import('@/features/services/ServiceDetailPage'))
const StoriesPage = lazy(() => import('@/features/stories/StoriesPage'))
const StoryPage = lazy(() => import('@/features/stories/StoryPage'))
const StoryEditorPage = lazy(() => import('@/features/stories/StoryEditorPage'))
const MyStoriesPage = lazy(() => import('@/features/stories/MyStoriesPage'))
const AlumniPage = lazy(() => import('@/features/alumni/AlumniPage'))
const AlumniProfilePage = lazy(() => import('@/features/alumni/AlumniProfilePage'))
const AlumniEditorPage = lazy(() => import('@/features/alumni/AlumniEditorPage'))
const MentorshipPage = lazy(() => import('@/features/mentorship/MentorshipPage'))
const ProjectsPage = lazy(() => import('@/features/projects/ProjectsPage'))
const ProjectDetailPage = lazy(() => import('@/features/projects/ProjectDetailPage'))
const NewProjectPage = lazy(() => import('@/features/projects/NewProjectPage'))
const AssistantPage = lazy(() => import('@/features/assistant/AssistantPage'))
const NotificationsPage = lazy(() => import('@/features/notifications/NotificationsPage'))
const ProfilePage = lazy(() => import('@/features/profile/ProfilePage'))
const ModerationPage = lazy(() => import('@/features/moderation/ModerationPage'))
const NotFoundPage = lazy(() => import('./NotFoundPage'))

export const routes: RouteObject[] = [
  {
    element: <Root />,
    errorElement: <RouteError />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <HomePage /> },
          { path: 'login', element: <GuestOnly><LoginPage /></GuestOnly> },
          { path: 'register', element: <GuestOnly><RegisterPage /></GuestOnly> },
          { path: 'forgot-password', element: <ForgotPasswordPage /> },
          { path: 'reset-password', element: <ResetPasswordPage /> },

          { path: 'opportunities', element: <OpportunitiesPage /> },
          { path: 'opportunities/:id', element: <OpportunityDetailPage /> },
          { path: 'services', element: <ServicesPage /> },
          { path: 'services/:id', element: <ServiceDetailPage /> },
          { path: 'stories', element: <StoriesPage /> },
          { path: 'stories/new', element: <RequireAuth><StoryEditorPage /></RequireAuth> },
          { path: 'stories/:id', element: <StoryPage /> },
          { path: 'stories/:id/edit', element: <RequireAuth><StoryEditorPage /></RequireAuth> },
          { path: 'my-stories', element: <RequireAuth><MyStoriesPage /></RequireAuth> },
          { path: 'alumni', element: <AlumniPage /> },
          { path: 'alumni/me', element: <RequireAuth><AlumniEditorPage /></RequireAuth> },
          { path: 'alumni/:id', element: <AlumniProfilePage /> },
          { path: 'mentorship', element: <RequireAuth><MentorshipPage /></RequireAuth> },
          { path: 'projects', element: <ProjectsPage /> },
          { path: 'projects/new', element: <RequireAuth><NewProjectPage /></RequireAuth> },
          { path: 'projects/:id', element: <ProjectDetailPage /> },
          { path: 'assistant/:conversationId?', element: <RequireAuth><AssistantPage /></RequireAuth> },
          { path: 'notifications', element: <RequireAuth><NotificationsPage /></RequireAuth> },
          { path: 'profile', element: <RequireAuth><ProfilePage /></RequireAuth> },
          { path: 'moderation', element: <RequireAuth moderator><ModerationPage /></RequireAuth> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]
