# Kakuma ResourceLink — API Contract (v1)

Base URL: `{HOST}/api/v1/` · JSON only (multipart for uploads) · OpenAPI: `GET /api/v1/schema/` (Swagger UI: `/api/v1/docs/`). The exported schema lives in `docs/api/openapi.yaml`, and the Android and React clients are built against it.

## Conventions

| Topic | Rule |
|---|---|
| Auth | `Authorization: Bearer <access>`. Access token TTL 15 min, refresh 7 days, **rotated** on every refresh; the old refresh token is blacklisted. |
| IDs | UUID strings. |
| Language | `Accept-Language: en|sw|es|it|de` localizes server messages. Content may be filtered with `?language=`. |
| Pagination | `?page=N&page_size=M` (max 50). Response: `{ "count", "next", "previous", "results": [] }`. |
| Timestamps | ISO-8601 UTC. Dates (`deadline`) `YYYY-MM-DD`. |
| Errors | Every non-2xx response: `{ "error": { "code": "validation_error", "message": "Human readable", "details": { "field": ["msg"] } } }` |
| Status codes | 200 OK · 201 Created · 204 No Content · 400 validation · 401 unauthenticated/expired token · 403 forbidden · 404 not found · 409 conflict (state) · 429 throttled · 503 AI unavailable |
| Rate limits | auth endpoints: 10/min per IP · AI chat: 20/hour per user · anon reads 120/min · user 600/min |

Error codes: `validation_error`, `authentication_failed`, `not_authenticated`, `token_not_valid`, `permission_denied`, `not_found`, `throttled`, `conflict`, `invalid_transition`, `ai_unavailable`, `ai_error`, `server_error`.

## Authentication — `/auth/`

| Method | Path | Auth | Body → Response |
|---|---|---|---|
| POST | `/auth/register/` | – | `{email, password, full_name, role: member|alumni|mentor, preferred_language}` → 201 `{user, tokens:{access, refresh}}` |
| POST | `/auth/login/` | – | `{email, password}` → 200 `{user, tokens}` · 401 bad credentials |
| POST | `/auth/token/refresh/` | – | `{refresh}` → 200 `{access, refresh}` (rotated) · 401 |
| POST | `/auth/logout/` | Bearer | `{refresh}` → 204 (refresh blacklisted) |
| GET/PATCH | `/auth/me/` | Bearer | → `User` ; PATCH `{full_name, preferred_language, profile:{bio, location}}` |
| POST | `/auth/me/avatar/` | Bearer | multipart `avatar` (jpg/png/webp ≤ 5 MB) → `User` |
| POST | `/auth/password-reset/` | – | `{email}` → 202 always (no account enumeration) |
| POST | `/auth/password-reset/confirm/` | – | `{uid, token, new_password}` → 204 |
| POST | `/auth/change-password/` | Bearer | `{old_password, new_password}` → 204 |

`User`: `{id, email, full_name, role, preferred_language, date_joined, profile:{bio, location, avatar}, has_alumni_profile}`

## Opportunities

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/opportunities/` | optional | filters: `category`, `search`, `is_remote`, `deadline_after`, `deadline_before`, `open=true` (deadline ≥ today or none), `bookmarked=true` (auth), `language`, `ordering=deadline|-created_at` |
| GET | `/opportunities/{id}/` | optional | |
| POST/DELETE | `/opportunities/{id}/bookmark/` | Bearer | POST → 201 `{bookmarked:true}` (idempotent, 200 if existed); DELETE → 204 |
| POST/PATCH/DELETE | `/opportunities/…` | moderator/admin | content management |

`Opportunity`: `{id, title, category, organization, summary, description, requirements:[str], location, is_remote, deadline, application_url, language, is_verified, is_bookmarked, created_at}`

## Services

`GET /services/` (filters `category`, `search`, `is_verified`), `GET /services/{id}/`. Write: moderator/admin.
`Service`: `{id, name, category, organization, description, phone, email, website, address, latitude, longitude, opening_hours, is_verified, last_verified_at}`

## Kakuma Stories

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/stories/` | optional | published only. filters `category`, `search`, `author`, `language`, `ordering=-published_at|-likes_count`. `?mine=true` (auth) returns **own stories, all statuses** (drafts). `?bookmarked=true` |
| POST | `/stories/` | Bearer | multipart or JSON `{title, body, excerpt?, category, language, cover_image?, consent_to_publish}` → 201 draft |
| GET | `/stories/{id}/` | optional | published, or own/any for moderators |
| PATCH/DELETE | `/stories/{id}/` | author | editing a published story sends it back to review |
| POST | `/stories/{id}/publish/` | author | requires `consent_to_publish=true` → `pending_review` (or `published` for moderators / when review disabled). 409 if already published |
| POST/DELETE | `/stories/{id}/like/` | Bearer | → `{liked, likes_count}` |
| POST/DELETE | `/stories/{id}/bookmark/` | Bearer | → `{bookmarked}` |
| GET/POST | `/stories/{id}/comments/` | GET optional, POST Bearer | `{body}` (1–2000 chars) |
| DELETE | `/comments/{id}/` | author/moderator | |

`Story` (list): `{id, title, slug, excerpt, cover_image, category, language, status, author:{id, full_name, avatar, country}, reading_time, likes_count, comments_count, is_liked, is_bookmarked, published_at, created_at, updated_at}`; detail adds `body` (Markdown), `consent_to_publish`, `rejection_reason`.

**Body format:** CommonMark Markdown. Clients render it with raw HTML disabled.

## Diaspora / Alumni

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/alumni/` | optional | anon sees `visibility=public`; members see `public`+`members`. filters `search` (name/profession/skills/country), `country`, `is_mentor`, `skill` |
| GET | `/alumni/{id}/` | optional | same visibility rules; `contact_email` only if `show_contact_email` |
| GET/PUT/PATCH/DELETE | `/alumni/profile/` | Bearer | own profile. Creating requires `consent=true`. DELETE withdraws (removes) the profile |

`AlumniProfile`: `{id, user_id, full_name, avatar, country, city, profession, organization, skills:[], bio, is_mentor, mentorship_areas:[], willing_to_contribute, contribution_interests:[], visibility, contact_email|null}`

## Mentorship

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/mentorship/requests/` | Bearer | `?box=sent|received` (default both), `?status=` |
| POST | `/mentorship/requests/` | Bearer | `{mentor_id (alumni profile id), goal, help_areas:[career, technical, scholarships, entrepreneurship, other], message}` → 201. 400 if mentor not accepting; 409 if a pending request exists |
| GET | `/mentorship/requests/{id}/` | participant | |
| PATCH | `/mentorship/requests/{id}/` | participant | `{status, response_message?}` transitions: mentor `pending→accepted|declined`, `accepted→completed`; mentee `pending→cancelled`. Invalid → 409 `invalid_transition` |

## Community projects

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/projects/` | optional | `active`/`completed` only (+ own). filters `category`, `status`, `search`, `mine=true` |
| POST | `/projects/` | Bearer | → `pending_review` |
| GET/PATCH/DELETE | `/projects/{id}/` | owner (write) | |
| GET/POST | `/projects/{id}/contributions/` | Bearer for POST | `{type, description, amount?, currency?}`. `financial_pledge` requires `amount>0`. Status is always `pledged` at creation |

`Project` includes `contribution_summary: {total_contributions, pledged_amount, currency, by_type:{}}`, which is described as pledged, not received.

## Claude AI assistant

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/ai/chat/` | Bearer | `{message (1–4000), conversation_id?, language?, task?: chat|explain_opportunity|summarize_story|translate, target_id?, target_language?}` → 200 `{conversation_id, message:{id, role:"assistant", content, created_at}, sources:[{type, id, title}]}` · 503 `ai_unavailable` when no key is configured · 429 |
| GET | `/ai/conversations/` | Bearer | own, paginated |
| GET/DELETE | `/ai/conversations/{id}/` | owner | detail includes `messages[]` |

## Notifications

`GET /notifications/` (`?unread=true`) · `POST /notifications/{id}/read/` → 200 · `POST /notifications/read-all/` → 200 `{updated}` · `GET /notifications/unread-count/` → `{count}`
`Notification`: `{id, type, title, body, data:{}, is_read, created_at}`. Types: `mentorship_request`, `mentorship_update`, `story_published`, `story_rejected`, `story_comment`, `project_contribution`, `project_status`.

## Moderation (moderator/admin only)

`GET /moderation/stories/` (pending queue) · `POST /moderation/stories/{id}/approve/` · `POST /moderation/stories/{id}/reject/ {reason}` · `GET /moderation/projects/` · `POST /moderation/projects/{id}/approve|reject/` · `POST /moderation/reports/` (any user: `{target_type: story|comment|project, target_id, reason, details}`) · `GET /moderation/reports/` · `POST /moderation/comments/{id}/hide/`
