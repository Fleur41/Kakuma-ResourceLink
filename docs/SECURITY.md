# Security review

Review of the backend, Android app and web app before the hackathon submission (24 Nov 2026).
It covers what is protected, what the review changed, and what still has to happen before a real launch.

## How the review was done

| Check | Tool / method | Result |
|---|---|---|
| Django deployment settings | `manage.py check --deploy` with production settings | No issues |
| Python static analysis | `bandit -r apps config -ll` (tests and migrations excluded) | Only dev-settings findings (binding to `0.0.0.0` locally) |
| Python dependencies | `pip-audit -r requirements.txt` | **64 known advisories → fixed** (see below); now none |
| JS dependencies (shipped code) | `npm audit --omit=dev` | 0 vulnerabilities |
| JS dependencies (build tools) | `npm audit` | 4 moderate, build-time only (`vite-plugin-pwa` → `workbox-build` → `glob` → `minimatch`); not shipped to browsers |
| Authorization | Manual read of every view's `get_queryset` / permissions | Scoped correctly (details below) |
| Android | Manifest, network security config, backup rules, logging | One fix (cleartext in release) |
| Web | XSS sinks, Markdown rendering, redirects, token storage | No raw HTML sinks; see notes |
| AI assistant | System prompt, grounding, user-controlled text in the prompt | One fix (prompt fence escaping) |

## Fixed during this review

1. **Vulnerable dependencies.** `Pillow 11.3.0` had 59 advisories, and it parses every uploaded avatar and story cover. `djangorestframework 3.16.1` had 5. Upgraded to Pillow 13.0.0 and DRF 3.18.3. All 94 backend tests pass and `pip-audit` is clean.
2. **Prompt-fence escape in the AI assistant.** Story, opportunity and service text is placed inside `<platform_content>` / `<record>` tags, and the system prompt tells Claude never to follow instructions found there. A record containing `</platform_content>` could close that block early and make its text look like instructions. `grounding.neutralize()` now strips those fence tags from untrusted text, including text sent for translation, and a test covers it.
3. **Cleartext HTTP in Android release builds.** The network security config allowed `http://` to `10.0.2.2` / `localhost` in every build. Release builds are now HTTPS-only. The local-backend exception lives in `src/debug/res/xml/` and only applies to debug builds (verified by dumping both APKs).
4. **Non-deterministic OpenAPI schema.** Self-assignable roles were a Python `set`, so the schema changed order on every export. They are now an ordered tuple. This is not a vulnerability, but it keeps schema diffs reviewable.

## What is protected

**Authentication**
- Email + password with Django's password validators. Login, registration and reset are throttled (`auth` 10/min).
- JWT access tokens last 15 minutes. Refresh tokens last 7 days, **rotate on every use** and are blacklisted after rotation. Logout blacklists the refresh token.
- The password reset endpoint always returns the same 202 message, so it does not reveal which emails have accounts.
- Android: tokens are kept in `EncryptedSharedPreferences` backed by the Android Keystore and excluded from cloud backup and device transfer.
- Web: the access token lives in memory only. The refresh token is in `localStorage` (a trade-off for offline-first; see "before launch"). Concurrent 401s share one refresh, so a rotated token is never reused.

**Authorization**
- Stories: drafts, pending and rejected stories are visible only to their author (and moderators). Likes, bookmarks and comments work only on published stories.
- Alumni: profiles require explicit consent. Visibility can be public, members-only or hidden. Email is shown only if the alumnus opts in, and only to signed-in members.
- Mentorship: requests are visible only to the mentee and mentor. Requests can only target profiles visible to the requester, and status changes follow a server-side state machine.
- Community projects: pending projects are visible only to the owner. Only the project owner and moderators see all pledges; everyone else sees only their own. Pledges are commitments, not payments: no payment data is collected anywhere.
- Moderation endpoints require the moderator or admin role, and actions are written to an audit log.
- AI conversations are scoped to their owner.

**Input and content**
- Uploads: JPG/PNG/WebP only, at most 5 MB, checked by extension, content type and Pillow decoding.
- Story Markdown is rendered without raw HTML on the web (`skipHtml`), and only `http(s)`, `mailto` and relative links are allowed. The Android app renders it natively, so there is no WebView.
- The web app has no `dangerouslySetInnerHTML`. Redirect-after-login accepts same-site paths only (tested against `//evil`, `https://evil`, `/\evil`).

**Transport and headers (production settings)**
- HTTPS redirect, HSTS (1 year, preload), secure cookies, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: same-origin`.
- CORS and CSRF trusted origins are explicit allow-lists from the environment.

**AI assistant**
- The Claude API key exists only on the server. Clients call `/api/v1/ai/chat/` and never talk to Anthropic directly.
- Answers are grounded only on published records. The system prompt forbids inventing listings and forbids decisions about refugee status or eligibility.
- Without a key, the endpoint returns `503 ai_unavailable` rather than a made-up answer. AI calls have their own throttle (20/hour per user).
- The Android app logs HTTP only in debug builds, and redacts the `Authorization` header.

## Before a real launch

These are deliberate hackathon-scope decisions or tasks that need infrastructure:

- **Web hosting headers — done for the Docker image.** `frontend/Dockerfile` serves the app through nginx with a strict CSP (`script-src 'self'`, no inline scripts), `frame-ancestors 'none'`, `nosniff`, `Referrer-Policy` and `Permissions-Policy`, and proxies `/api` on the same origin. This is the main mitigation for the refresh token being in `localStorage`. Remaining: enable HSTS once TLS is in front (commented line in `frontend/security-headers.conf`). A stronger option is to move the web refresh token to an `HttpOnly; Secure; SameSite=Strict` cookie.
- **Media serving.** Django serves `/media/` only in DEBUG. In production, serve uploads from a reverse proxy or object storage with `Content-Disposition`/`nosniff` and no script execution.
- **Secrets.** Set `DJANGO_SECRET_KEY`, the database password and `ANTHROPIC_API_KEY` from a secret manager. Never reuse `.env.example` values.
- **Demo data.** `seed_demo` creates accounts with a shared demo password. Never run it against a production database.
- **Email.** Configure a real SMTP backend for password reset (the dev backend prints to the console).
- **Android release — signing config done.** Keys come from `keystore.properties` or environment variables (see `mobile/README.md`). Remaining: create and back up the real upload key, and build with `-Pkakuma.apiUrl` set to the real HTTPS API.
- **Content verification.** All seed content is illustrative and marked unverified. Real listings need a verification workflow before being shown as "Verified".
- **Monitoring.** Add error tracking and alerting for repeated auth failures and moderation reports.
