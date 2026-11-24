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
python manage.py seed_testdata  # full fictional test dataset + 18 test accounts (password Admin@123)
python manage.py runserver 0.0.0.0:8000
```

- API root: `http://localhost:8000/api/v1/`. For the Android app, run `adb reverse tcp:8000 tcp:8000` first ([why](mobile/README.md#run-it-locally)).
- Swagger UI: `http://localhost:8000/api/v1/docs/`
- Admin: `http://localhost:8000/admin/` (`python manage.py createsuperuser`)
- Test accounts (local only, all with the password `Admin@123`). The command prints the full list.

  | Role | Accounts |
  |---|---|
  | Admin (also Django `/admin/`) | `admin@kakuma.test` |
  | Moderator | `esther.wanjiru@kakuma.test` (Kiswahili), `moderator@demo.kakuma.test` |
  | Member | `amina@demo.kakuma.test`, `deng.majok@kakuma.test`, `halima.abdi@kakuma.test` (Kiswahili), `ahmed.yusuf@kakuma.test`, `sara.tesfaye@kakuma.test`, `nyabol.gatluak@kakuma.test`, `jean.habimana@kakuma.test` |
  | Mentor (alumni) | `achol.deng@kakuma.test` (Italiano), `abdirahman.hassan@kakuma.test` (Deutsch), `daniel@demo.kakuma.test`, `peter@demo.kakuma.test` |
  | Alumni | `joseph.lokwang@kakuma.test` (Español), `mary.nyandeng@kakuma.test`, `ibrahim.omar@kakuma.test` (hidden profile), `grace@demo.kakuma.test` |

  The data covers every state: stories in review, draft and needing changes, mentorship requests in all five states, projects awaiting approval, pledges, and closed or rolling deadlines. `seed_demo` alone creates only the small original demo set. Both commands are safe to re-run and refuse to run when `DEBUG` is off.

Run the whole stack in Docker instead: `docker compose --profile full up --build` (API on :8000, production web build with security headers on http://localhost:8080).

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

### Server translations

Emails, API error messages and notifications are translated on the server (`backend/locale/`, sw/es/it/de). Notifications use each recipient's preferred language; API errors follow the `Accept-Language` header that both clients send. After changing a translatable string:

```bash
cd backend
python manage.py makemessages -l sw -l es -l it -l de --ignore=.venv
# translate the new entries in locale/<lang>/LC_MESSAGES/django.po, then
python manage.py compilemessages --ignore=.venv   # commit both .po and .mo
```

## Web app quick start

```bash
cd frontend && npm install && npm run dev    # http://localhost:5173, proxies /api to the backend
npm test                                     # Vitest suite
```

See [`frontend/README.md`](frontend/README.md) for details.

## Tests at a glance

| Part | Command | Tests |
|---|---|---|
| Backend | `cd backend && pytest` | 98 (97% coverage, PostgreSQL) |
| Android | `cd mobile && ./gradlew testDebugUnitTest` (+ `connectedDebugAndroidTest` on an emulator) | 61 unit + 6 instrumented |
| Web | `cd frontend && npm test` | 48 |

## Team

| Name | GitHub |
|---|---|
| Fleur41 | [github.com/Fleur41](https://github.com/Fleur41) |
| Halkano Malise | [github.com/malise5](https://github.com/malise5) |

## Content policy

All seed data is **illustrative sample content**. Organisations are generic, nothing is marked verified, and people are fictional. Replace it with verified content before a real launch.
