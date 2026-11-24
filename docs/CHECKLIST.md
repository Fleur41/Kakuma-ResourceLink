# Development checklist

Legend: [x] done & verified · [~] in progress · [ ] not started

## Phase 1 — Specification
- [x] Repo + environment inspection (Python 3.11, Node, Android Studio JDK, Docker)
- [x] Architecture, ERD, roadmap — `docs/architecture/ARCHITECTURE.md`
- [x] API contract — `docs/api/API_CONTRACT.md`
- [x] Design system spec — `docs/designs/DESIGN_SYSTEM.md`

## Phase 2 — Backend  ✅ (96 tests passing on PostgreSQL 16, 97% coverage)
- [x] Project skeleton, split settings (base/development/test/production), Docker + Postgres, .env.example
- [x] common: UUID base model, pagination, error envelope, upload validators, throttles
- [x] accounts: custom user, profile, register/login/refresh(rotation+blacklist)/logout/me/avatar/password reset/change
- [x] opportunities + bookmarks
- [x] services directory
- [x] stories, comments, likes, bookmarks, publish → review flow
- [x] moderation queue (stories, projects), comment hiding, reports, audit log
- [x] notifications (rendered in recipient language)
- [x] diaspora alumni profiles (consent/visibility)
- [x] mentorship requests (state machine, DB constraints)
- [x] community projects + contributions (pledges, privacy of contributor list)
- [x] Claude AI assistant (grounded, mocked tests, 503 without key)
- [x] OpenAPI export to docs/api/openapi.yaml (0 warnings)
- [x] Demo seed command (`seed_demo`)
- [x] Docker image builds
- [ ] Live Claude call verified with a real ANTHROPIC_API_KEY (needs key)
- [x] Translation catalogues (.po/.mo) for sw/es/it/de server messages: emails, API errors, notifications (129 strings each)

## Phase 3 — Android  ✅ (61 unit + 6 instrumented tests passing, lint 0 errors)
- [x] Gradle KTS + libs.versions.toml (AGP 9.3, Kotlin 2.4, compileSdk 37.1, minSdk 26)
- [x] Core: Hilt, network/auth refresh, Keystore token store, Room, DataStore, design system, UiEvent (short toasts)
- [x] Features: auth, home, opportunities, stories (+offline drafts), alumni, mentorship, assistant, profile, services, projects, notifications
- [x] WorkManager sync, notification channels, notification polling
- [x] Localization EN/SW/ES/IT/DE (401 strings each, per-app language)
- [x] Tests: use cases, ViewModels/Flows, DAO, repository, network, UI, navigation
- [x] Smoke-tested on an API 37 emulator against the local backend (sign-in, all tabs, Kiswahili)
- [ ] Assistant answers verified end to end (needs ANTHROPIC_API_KEY on the backend)
- [x] Release signing config (keystore.properties or env vars) + R8 rules reviewed; minified signed build run on emulator

## Phase 4 — React  ✅ (48 tests passing, typecheck + build clean, lint 0 errors)
- [x] Vite + TS skeleton, API client w/ single-flight refresh, protected routes, i18n
- [x] Pages per inventory: landing, dashboard, auth + password reset, opportunities, services, stories (editor with local autosave), alumni, mentorship, projects/pledges, assistant, notifications, profile, moderation
- [x] Localization EN/SW/ES/IT/DE (578 strings each), light/dark theme, PWA app shell + persisted query cache
- [x] Tests: API client, validation, cache, i18n parity, pages (login, opportunities, story, guards, language)
- [x] Smoke-tested in Chrome against the local backend (sign-in, stories, like, Kiswahili)
- [ ] Assistant answers verified end to end (needs ANTHROPIC_API_KEY on the backend)

## Phase 6 — Wrap-up  ✅
- [x] Security review — `docs/SECURITY.md` (deploy check, bandit, pip-audit, npm audit, manual authz review)
- [x] Fixes: Pillow 13.0.0 + DRF 3.18.3 (64 advisories), AI prompt-fence escaping, HTTPS-only Android release, stable OpenAPI enum order
- [x] README: docs index, test overview, web quick start
- [x] Demo script — `docs/DEMO_SCRIPT.md`
- [x] Web production image: nginx with CSP + security headers, same-origin API proxy (`docker compose --profile full up`, port 8080), verified in Chrome with no CSP violations
- [ ] Before launch: real domain + TLS (then enable HSTS in `frontend/security-headers.conf`), media via object storage, upload signing key, live AI check (see SECURITY.md)
