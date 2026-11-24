# Kakuma ResourceLink — Figma-ready Design System

Source material: `Kakuma_ResourceLink_UI_Reference_Board.png` plus the SVG design pack (12 Android frames at 390×844, 6 web frames at 1440×1000, `design-tokens.json`).
*No Figma file was created or edited. This is a Figma-ready specification, and the SVGs can be placed into Figma directly.*

## 1. Tokens

| Token | Value | Use |
|---|---|---|
| `color/primary` | #075B4B | primary buttons, app bars, selected nav, hero gradients (start) |
| `color/accent` | #12A681 | hero gradient end, success, active chips, links |
| `color/gold` | #F5A623 | highlights, badges ("Featured"), deadline chips, pledge amounts |
| `color/background` | #F6F7F4 | app/page background |
| `color/surface` | #FFFFFF | cards, sheets, inputs |
| `color/text` | #202B29 | primary text (contrast 14.6:1 on background) |
| `color/muted` | #66736F | secondary text (4.9:1) |
| `color/border` | #E3E9E5 | card & input outlines |
| `color/error` | #C62828 | errors |
| `color/primary-container` | #E3F2EE | soft chip/quick-access tiles |
| `color/gold-container` | #FFF3DC | category pill backgrounds |
| Dark scheme | bg #0F1A18 · surface #17241F · text #E7EEEB · primary #4FC3A1 · accent #12A681 | dark mode toggle (profile) |

**Typography:** Poppins for headings, Inter for body text.
Display 32/40 Bold · Heading 24/32 Bold · Title 18/26 SemiBold · Body 14/22 Regular · Label 14/20 Medium · Caption 12/18 Regular. Allow 30–40% growth for SW/DE strings, so text must never be truncated inside buttons.

**Spacing:** 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64. Screen gutter 20 (mobile) / 24–64 (web).
**Radii:** small 8 (chips), medium 12 (inputs, buttons), large 18 (cards), xl 24 (hero banners, sheets), pill 999.
**Elevation:** cards 0dp with a 1px border plus a soft shadow (0 2 8 rgba(7,91,75,.06)).
**Touch targets:** ≥ 48dp Android / 44px web.

## 2. Components (with variants)

| Component | Variants / states |
|---|---|
| Button | Primary (filled #075B4B), Secondary (outlined), Tonal (accent container), Text, Destructive. States: default, pressed, focused (2px accent ring), disabled (38% opacity), loading (spinner, label kept) |
| Text field | Label above, placeholder, leading icon optional, trailing action (show password). States: default, focused (primary border), error (red border + message below), disabled |
| Chip | Filter (selected = primary filled, white text; unselected = surface + border), Category pill (gold container) |
| Cards | OpportunityCard (icon tile, title, org, deadline, category pill, bookmark), StoryCard (cover image, title, author avatar, read time, views/likes), AlumniCard (initial avatar, name, profession, country, skill tags, "View profile"), ServiceCard, ProjectCard (progress of pledges), NotificationRow |
| Hero banner | Gradient primary→accent, radius 24, headline + CTA pill + image |
| Quick-access tile | 56px rounded tile with icon + caption (Opportunities, Services, Stories, AI Assistant) |
| Navigation | Android: 5-tab bottom bar (Home, Explore, Stories, Community, Profile). Web: top nav for public pages; left sidebar (primary #075B4B) for authenticated dashboard |
| Chat | Assistant bubble (surface, left), user bubble (primary, right), suggested prompt chips, composer with round send button |
| Feedback | Skeleton shimmer blocks per card type, EmptyState (illustration icon, title, body, CTA), ErrorState (message + Retry), OfflineBanner (gold), Snackbar, Toast, ConfirmDialog |
| Status badge | Draft (grey), In review (gold), Published (accent), Rejected (error), Pending/Accepted/Declined for mentorship |

## 3. Screen inventory

**Android:** Splash · Onboarding/Welcome · Language selection · Login · Register · Forgot password · Home dashboard · Opportunities list (chips: All/Education/Jobs/Training) · Opportunity detail · Services list/detail · Stories feed (Popular/Latest/Inspiring) · Story detail · Story editor · My stories/drafts · Alumni directory · Alumni profile · Alumni profile editor (consent) · Mentorship request form · Mentorship requests (sent/received) · Community projects list/detail/pledge · AI assistant + history · Notifications · Profile & settings (language, dark mode, offline downloads, logout) · Offline / empty / error / confirmation states.

**Web:** Landing · Login · Register · Dashboard (sidebar, stat tiles, featured opportunities) · Opportunities (filter sidebar + list) · Opportunity detail (requirements, Apply Now) · Services · Stories feed · Article page · Story editor (with publishing checklist) · Alumni directory/profile · Mentorship · Community projects · AI assistant · Notifications · Profile/settings · Moderation queue.

## 4. Prototype flows
1. Register → Language → Home
2. Home → Opportunities → Detail → Bookmark (toast "Saved")
3. Stories feed → Article → Author profile
4. Editor → Save draft → Submit for review → "In review" badge
5. Alumni → Profile → Request mentorship → Confirmation
6. AI assistant → Suggested question → Answer with sources
7. Project → Pledge contribution → "Pledged (not a payment)" confirmation

## 5. Content rules
Use illustrative sample content marked as sample. Never present invented organizations or scholarships as verified (`is_verified=false` for seeds). Diaspora profiles and story publishing show explicit consent controls. Moderation status is always visible to the author.
