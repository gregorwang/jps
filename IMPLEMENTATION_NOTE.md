# Anime Japanese Lab Implementation Note

## Current stage

This project is in the v0.1 -> v0.1.5 personal-use phase. It now supports two progress modes:

- Anonymous device-level mode: the browser keeps a generated `device-*` id in `localStorage`; no login is required.
- Owner-only sync mode: a single owner email can log in with a self-built Worker auth flow and sync progress by `user_id`.

This is not Supabase Auth, Clerk, Auth0, Better Auth, or an open registration system. Login exists only for personal Windows/iPad progress sync.

## Completed v0.1/v0.1.5 pieces

- React + Vite + TanStack Router app deployed on Cloudflare Workers.
- Supabase-backed corpus and learning material reads for works, episodes, vocab, grammar, sentences, exercises, and subtitles.
- Cloudflare Vectorize RAG search with Workers AI embeddings using `@cf/baai/bge-m3`.
- Generative AI calls through Cloudflare AI Gateway only.
- AI result cache in Supabase for vocab/grammar/sentence explanations, sentence deep dives, read-air analysis, sentence corrections, and character profiles.
- Device-level progress persistence in Supabase with localStorage fallback.
- Today review queue for `unknown`, `bad`, `fuzzy`, and `ok` states.
- History page for correction history, AI interactions, and character profile cache entries.
- Owner-only minimal auth tables and Worker APIs.
- `/login` and `/account` pages for owner login, device claim, logout, and password change.
- PWA basics: manifest, icon, service worker shell cache, API bypass, update prompt, and iPad landscape smoke coverage.

## Owner-only auth model

The auth system is intentionally small:

- `POST /api/auth/register-owner` initializes the first owner only.
- The email must equal Worker env `OWNER_EMAIL`.
- Passwords are stored as PBKDF2-SHA-256 hashes with random salt and iteration count.
- Session tokens are high-entropy random values.
- Only `session_token_hash` is stored in Supabase.
- The raw session token is stored only in an `HttpOnly; Secure; SameSite=Lax; Path=/` cookie.
- Tokens are not stored in `localStorage`.
- `POST /api/auth/login` has a simple in-memory IP/email failure limiter.

Production must configure:

- `OWNER_EMAIL` as a Worker variable.
- `SUPABASE_SERVICE_ROLE_KEY` as a Wrangler secret.

Without these, anonymous mode still works, but owner initialization/login cannot complete.

## Data ownership

Unauthenticated reads and writes use `device_id`.

Authenticated reads and writes use `user_id` first, while still keeping `device_id` as source metadata when the request includes it.

User-scoped tables:

- `user_progress`
- `sentence_correction_history`
- `ai_interaction_history`
- `app_users`
- `app_sessions`

Global cache tables:

- `ai_result_cache`
- `character_language_profiles`

The global AI cache stores reusable model outputs keyed by normalized input. `ai_interaction_history` stores the owner/device history pointer and a result snapshot for personal history pages.

## Device claim merge

`POST /api/auth/claim-device` requires login and accepts the current `deviceId`.

Merge behavior:

- Device progress rows are bound to the current `user_id`.
- If the same `item_id` already exists for the user, the newer `last_reviewed_at` wins.
- If timestamps are tied or missing, the weaker review state wins priority for review safety.
- Correction history and AI interaction history rows for the device are patched with `user_id`.
- Re-running claim is intended to be idempotent and should not create duplicate progress rows.

## Still out of scope

- Open signup or multi-user permission design.
- User center, avatar, nickname, or social features.
- Supabase Auth or any full authentication product.
- Automatic OCR handwriting judgment.
- Automatic pronunciation scoring.
- Full offline learning packs.
- Rebuilding the app or changing `srt/` and data tooling directory structure.

## Deployment and smoke status

Latest deployed Worker:

`https://anime-japanese-lab.ishallnotwant123.workers.dev`

Validated locally:

- `pnpm build`
- `pnpm test`
- `pnpm lint`
- `pnpm test:smoke`

Validated live after deployment:

- `/api/auth/me` returns `{ "user": null }` when not logged in.
- `/api/auth/logout` clears the cookie and returns ok.
- `/api/auth/claim-device` returns 401 when not logged in.
- `/api/review/today` works with anonymous `deviceId`.
- `/api/history` works with anonymous `deviceId`.
- `/api/rag/search` works with UTF-8 JSON and records device AI history.
- `/api/ai/explain` works through AI Gateway.

Current production caveat: `OWNER_EMAIL` and `SUPABASE_SERVICE_ROLE_KEY` are not configured in the deployed Worker bindings shown by Wrangler, so real owner registration/login still needs those env values before final auth smoke.

## PWA and cache cleanup

The service worker bypasses all `/api/` requests, including:

- `/api/auth/*`
- `/api/ai/*`
- `/api/progress/*`
- `/api/review/*`
- `/api/history/*`

After `pnpm run deploy`, browsers may still use an older JS bundle or service worker.

To clear local browser state during testing:

1. Open DevTools.
2. Go to Application.
3. Unregister the service worker for the site.
4. Clear Cache Storage.
5. Clear Local Storage only if you want to reset the anonymous `deviceId`.
6. Hard refresh the page.

On iPad Safari/PWA, remove the home-screen app if installed, clear Safari website data for the Workers domain, then add the app to the home screen again.
