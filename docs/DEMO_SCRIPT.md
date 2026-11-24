# Demo script (about 7 minutes)

One story to tell: **Amina, a young woman in Kakuma, finds an opportunity, gets a mentor and shares her own story, on the web and on her phone, in her own language, with one account.**

## Before you present (10 minutes earlier)

```bash
# 1. Backend + database + demo data
cd backend && docker compose up -d db && source .venv/bin/activate
python manage.py migrate && python manage.py seed_testdata  # all accounts: Admin@123
python manage.py runserver 0.0.0.0:8000

# 2. Web app (new terminal)
cd frontend && npm run dev                                   # http://localhost:5173

# 3. Android (new terminal): start the emulator, then
adb reverse tcp:8000 tcp:8000
cd mobile && ./gradlew installDebug
```

- Set `ANTHROPIC_API_KEY` in `backend/.env` to show live AI answers. Without it, show the "assistant unavailable" state and explain why that is the honest behaviour.
- Open two browser windows: a normal one (Amina) and a private one (Daniel, then the moderator).
- Sign out of the Android app beforehand so you can show the sign-in screen.

| Account | Role | Used for |
|---|---|---|
| `amina@demo.kakuma.test` | Community member | Main persona |
| `daniel@demo.kakuma.test` | Alumni mentor (Germany) | Accepts the mentorship request |
| `moderator@demo.kakuma.test` | Moderator | Approves Amina's story |

## The demo

**1. The problem (30 s)** — slides or speak
Information in Kakuma is scattered across notice boards, WhatsApp groups and word of mouth. Alumni abroad want to help but have no channel to do it. Many people read in a second language and have weak connectivity.

**2. Landing page as a guest (30 s)** — web
- Open `localhost:5173`: the hero, the three pillars, open opportunities and stories.
- Point out the **"Unverified" / sample content** labels: we never present illustrative data as real.

**3. Find an opportunity (1 min)** — web, signed in as Amina
- Sign in and show the short **"Welcome back, Amina"** toast and the dashboard.
- Go to **Opportunities**, filter by *Scholarship*, and show the deadline chip ("30 days left").
- Bookmark it; the short toast says "Saved". Open it and click **Explain with AI**: the assistant explains the listing in plain words, grounded only on the listing, and does not judge eligibility.

**4. Same account on the phone, in Kiswahili (1 min)** — Android
- Sign in on the emulator as Amina. The bookmark she just made on the web is already under saved opportunities: **one backend, one database, one auth system**.
- Profile → Language → **Kiswahili**: the whole app switches.
- Turn on airplane mode: saved opportunities and stories still open, and the offline banner appears. Turn it off again.

**5. Ask a mentor (1 min)** — web
- **Alumni** → open *Daniel M.* (software engineer, Germany) → **Request mentorship**, pick "Career" and add a short message. Toast: "Request sent".
- Private window: sign in as Daniel → **Mentorship → Received** → **Accept**.
- Back to Amina: the **notification bell** shows the update and links straight to the request.

**6. Share a story (1 min)** — Android or web
- **Write a story**: title, a few paragraphs, cover photo, consent box. Point out the checklist and the privacy tip ("don't share case numbers").
- On the web, show that the draft **autosaves locally**, so a dropped connection doesn't lose it.
- **Submit for review**: the status is *In review*.

**7. Moderation (45 s)** — web, private window as the moderator
- **Moderation → Stories** → read → **Approve**. Amina gets a notification in her language and the story is now public.
- Mention that rejection requires a kind, specific reason, which is shown only to the author.

**8. Give back (45 s)** — web
- **Community → Solar study lamps** → **Pledge support** → *Volunteering*.
- The confirmation says **"Pledged (not a payment)"**: no money moves through the platform, and the project owner follows up.

**9. Close (30 s)**
- Five languages, offline-first, the same data on web and Android, and AI that only speaks from verified platform content.
- Built with Django + PostgreSQL, Kotlin + Jetpack Compose, and React + TypeScript. About 200 automated tests across the three apps.
- Next steps: partner-verified listings, SMS/USSD for basic phones, and real deployment ([docs/SECURITY.md](SECURITY.md#before-a-real-launch)).

## If something goes wrong

| Symptom | Fix |
|---|---|
| Android shows "No connection" | Run `adb reverse tcp:8000 tcp:8000` again (it resets when the emulator restarts) |
| Web shows "Session expired" | Sign in again; refresh tokens rotate, and an old tab may hold a used one |
| Assistant says unavailable | `ANTHROPIC_API_KEY` is not set, or the AI throttle (20/hour) was hit |
| Demo data looks changed | `seed_testdata` is idempotent: it won't reset edits. For a clean slate: `python manage.py flush` then `seed_testdata` (local database only) |
