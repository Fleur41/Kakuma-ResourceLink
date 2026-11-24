# Kakuma ResourceLink — Web app

React + TypeScript client for the Kakuma ResourceLink API. It uses the same backend, PostgreSQL database and accounts as the Android app.

| | |
|---|---|
| Framework | React 19 · react-router 8 (data router) · Vite 8 · TypeScript 6 |
| Styling | Tailwind CSS 4 with design tokens (`src/styles/index.css`), Poppins + Inter, light/dark |
| Data | TanStack Query 5, persisted to `localStorage` for offline reading |
| i18n | i18next: English, Kiswahili, Español, Italiano, Deutsch (`src/i18n/locales/`) |
| Offline | PWA service worker caches the app shell; cached API data is shown with an offline banner |
| Tests | Vitest · Testing Library · MSW |

## Run it locally

Requirements: Node 22.22+ and the backend running on `http://127.0.0.1:8000` (see the [root README](../README.md#backend-quick-start)).

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173 — /api and /media are proxied to the backend
```

Sign in with a demo account from `python manage.py seed_demo` (e.g. `amina@demo.kakuma.test`; the password is printed by the command).

To point the app at another API host, copy `.env.example` to `.env` and set `VITE_API_URL` (e.g. `https://api.example.org/api/v1`). That host must allow the web origin in `CORS_ALLOWED_ORIGINS`.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with proxy |
| `npm run build` | Type-check and build to `dist/` (with service worker) |
| `npm run preview` | Serve the production build |
| `npm test` | Run the test suite once |
| `npm run typecheck` / `npm run lint` | TypeScript / ESLint |
| `npm run gen:api` | Regenerate `src/api/schema.d.ts` from `docs/api/openapi.yaml` |

## How it is put together

```
src/
  api/          fetch client (JWT refresh, error envelope), typed endpoints, generated schema
  app/          router, query client + persistence, error pages
  components/   ui/ (buttons, fields, dialogs, toasts, states) and layout/ (shell, nav)
  features/     one folder per area: auth, opportunities, services, stories, alumni,
                mentorship, projects, assistant, notifications, profile, moderation, home
  i18n/         i18next setup and the five locale files
  lib/          formatting, hooks, theme, cache helpers
  test/         MSW server, fixtures, render helper
```

- **Auth.** The access token is kept in memory only; the refresh token is kept in `localStorage`. Concurrent 401s share one refresh call, because refresh tokens rotate. A rejected refresh signs the user out with a short toast and sends them to sign in, returning them afterwards to where they were.
- **Offline.** Lists and details you have opened are persisted and shown when offline; actions that need the network show a clear error. Assistant and moderation data are never persisted.
- **Safety.** Story Markdown is rendered without raw HTML, and only `http(s)`, `mailto` and relative links are allowed. Redirects after sign-in accept same-site paths only.
- **Feedback.** Quick confirmations (saved, liked, sent) use short toasts; errors appear inline next to the field or action.

## Known limitations

- The AI assistant needs `ANTHROPIC_API_KEY` on the backend. Without it, the page shows an "unavailable" message.
- `npm audit` reports 4 moderate issues in build-time dependencies only (`vite-plugin-pwa` → `workbox-build` → `glob` → `minimatch`). They are not shipped to the browser.
