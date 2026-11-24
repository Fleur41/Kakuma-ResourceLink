import {
  Bell,
  Bot,
  BookOpen,
  Briefcase,
  HandHeart,
  LayoutDashboard,
  LifeBuoy,
  ShieldCheck,
  UserRound,
  Users,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  to: string
  labelKey: string
  icon: LucideIcon
  end?: boolean
}

/** Guest top navigation (design: Explore · Stories · Alumni · Community). */
export const publicNav: NavItem[] = [
  { to: '/opportunities', labelKey: 'nav.explore', icon: Briefcase },
  { to: '/services', labelKey: 'nav.services', icon: LifeBuoy },
  { to: '/stories', labelKey: 'nav.stories', icon: BookOpen },
  { to: '/alumni', labelKey: 'nav.alumni', icon: Users },
  { to: '/projects', labelKey: 'nav.community', icon: HandHeart },
]

/** Signed-in sidebar. */
export const memberNav: NavItem[] = [
  { to: '/', labelKey: 'nav.dashboard', icon: LayoutDashboard, end: true },
  { to: '/opportunities', labelKey: 'nav.opportunities', icon: Briefcase },
  { to: '/services', labelKey: 'nav.services', icon: LifeBuoy },
  { to: '/stories', labelKey: 'nav.stories', icon: BookOpen },
  { to: '/alumni', labelKey: 'nav.alumni', icon: Users },
  { to: '/mentorship', labelKey: 'nav.mentorship', icon: UsersRound },
  { to: '/projects', labelKey: 'nav.community', icon: HandHeart },
  { to: '/assistant', labelKey: 'nav.assistant', icon: Bot },
  { to: '/notifications', labelKey: 'nav.notifications', icon: Bell },
  { to: '/profile', labelKey: 'nav.profile', icon: UserRound },
]

export const moderationNav: NavItem = { to: '/moderation', labelKey: 'nav.moderation', icon: ShieldCheck }
