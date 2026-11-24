# Kakuma-ResourceLink

> **Connecting Opportunities, Inspiring Generations, Transforming Kakuma.**

A multilingual (English, Kiswahili, Español, Italiano, Deutsch), offline-first platform connecting current and former Kakuma residents with opportunities, essential services, inspiring stories, mentorship and community projects.

| Part | Stack | Folder | Status |
|---|---|---|---|
| Backend API | Django 5.2 · DRF · PostgreSQL 16 · Simple JWT · Claude API | `backend/` | ✅ Implemented & tested |
| Android app | Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Room · Retrofit | `mobile/` | ✅ Implemented & tested ([guide](mobile/README.md)) |
| Web app | React 19 · TypeScript · Vite · TanStack Query · Tailwind | `frontend/` | ✅ Implemented & tested ([guide](frontend/README.md)) |

Both clients share one backend, one PostgreSQL database and one authentication system. No Firebase. The Claude API key never leaves the server.

## Documentation

- Architecture, ERD & roadmap: [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md)
- API contract: [`docs/api/API_CONTRACT.md`](docs/api/API_CONTRACT.md) · OpenAPI schema: [`docs/api/openapi.yaml`](docs/api/openapi.yaml)
- Design system & screen inventory: [`docs/designs/DESIGN_SYSTEM.md`](docs/designs/DESIGN_SYSTEM.md)
- Security review: [`docs/SECURITY.md`](docs/SECURITY.md)
- Demo script: [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md)
- Progress checklist: [`docs/CHECKLIST.md`](docs/CHECKLIST.md)

## Backend quick start

Requirements: Python 3.11+, Docker.

```bash
cd backend
cp .env.example .env            # then set DJANGO_SECRET_KEY (and ANTHROPIC_API_KEY to enable the assistant)
docker compose up -d db         # PostgreSQL 16 on localhost:5432
python3.11 -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
python manage.py migrate
python manage.py seed_demo      # illustrative sample data + demo accounts
python manage.py runserver 0.0.0.0:8000
```

- API root: `http://localhost:8000/api/v1/`. For the Android app, run `adb reverse tcp:8000 tcp:8000` first ([why](mobile/README.md#run-it-locally)).
- Swagger UI: `http://localhost:8000/api/v1/docs/`
- Admin: `http://localhost:8000/admin/` (`python manage.py createsuperuser`)
- Demo accounts (local only): `amina@demo.kakuma.test`, `daniel@demo.kakuma.test` (mentor), `moderator@demo.kakuma.test`. The password is printed by `seed_demo`.

Run the whole stack in Docker instead: `docker compose --profile full up --build`.

### Tests

```bash
cd backend && pytest            # runs against PostgreSQL; Claude is always mocked
pytest --cov=apps               # coverage report
```

### Backend modules

`accounts` (email login, JWT with refresh rotation and blacklist, password reset, profiles) · `opportunities` (+ bookmarks) · `services` · `stories` (drafts → review → publish, likes, comments, bookmarks) · `diaspora` (consent-based alumni profiles) · `mentorship` (request state machine) · `contributions` (community projects and pledges; pledges are commitments, not payments) · `notifications` · `moderation` (review queues, reports, audit log) · `ai_assistant` (Claude, grounded on platform data).

### Claude assistant

- `POST /api/v1/ai/chat/` supports `chat`, `explain_opportunity`, `summarize_story` and `translate`.
- Answers are grounded on published platform records retrieved for each question. The system prompt forbids inventing listings and forbids refugee-status or eligibility decisions.
- The default model is `claude-opus-5-5`, configurable with `AI_MODEL` and `AI_EFFORT`. Server-side refusal fallback is enabled (`AI_ENABLE_FALLBACKS`).
- Without `ANTHROPIC_API_KEY` the endpoint returns `503 ai_unavailable`. It never returns a fabricated answer.

## Web app quick start

```bash
cd frontend && npm install && npm run dev    # http://localhost:5173, proxies /api to the backend
npm test                                     # Vitest suite
```

See [`frontend/README.md`](frontend/README.md) for details.

## Tests at a glance

| Part | Command | Tests |
|---|---|---|
| Backend | `cd backend && pytest` | 94 (97% coverage, PostgreSQL) |
| Android | `cd mobile && ./gradlew testDebugUnitTest` (+ `connectedDebugAndroidTest` on an emulator) | 61 unit + 6 instrumented |
| Web | `cd frontend && npm test` | 48 |

## Content policy

All seed data is **illustrative sample content**. Organisations are generic, nothing is marked verified, and people are fictional. Replace it with verified content before a real launch.
