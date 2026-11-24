# Development checklist

Legend: [x] done & verified · [~] in progress · [ ] not started

## Phase 1 — Specification
- [x] Repo + environment inspection (Python 3.11, Node, Android Studio JDK, Docker)
- [x] Architecture, ERD, roadmap — `docs/architecture/ARCHITECTURE.md`
- [x] API contract — `docs/api/API_CONTRACT.md`
- [x] Design system spec — `docs/designs/DESIGN_SYSTEM.md`

## Phase 2 — Backend  ✅ (93 tests passing on PostgreSQL 16, 97% coverage)
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
- [ ] Translation catalogues (.po) for sw/es/it/de server messages (clients localize UI strings)

## Phase 3 — Android
- [ ] Gradle KTS + libs.versions.toml
- [ ] Core: Hilt, network/auth refresh, Keystore token store, Room, DataStore, design system, UiEvent
- [ ] Features: auth, home, opportunities, stories (+offline drafts), alumni, mentorship, assistant, profile, services, projects, notifications
- [ ] WorkManager sync, notifications channels
- [ ] Localization EN/SW/ES/IT/DE
- [ ] Tests

## Phase 4 — React
- [ ] Vite + TS skeleton, API client w/ refresh, protected routes, i18n
- [ ] Pages per inventory
- [ ] Tests

## Phase 6 — Wrap-up
- [ ] Security review · README · Demo script
