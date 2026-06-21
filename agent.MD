# Agent Rules

- Do not call Playwright in this project unless the user explicitly asks for browser automation or E2E testing. Prefer TypeScript, build, lint, or unit tests for routine verification to avoid unnecessary token and runtime cost.
- Exception: if the user reports a UI/rendering mismatch, especially "backend/API returns data but the frontend does not show it", use browser verification before declaring the issue fixed. Check the actual rendered DOM text, visible controls, network responses, console errors, and service worker/cache behavior. A passing build plus a correct API response is not enough for UI bugs.

## Multi-Work Corpus UI Rules

- Treat Supabase/Worker API data as the source of truth. Do not infer current work slugs or episode counts from stale local CSV exports or frontend fallback fixtures when diagnosing production behavior.
- For multi-work pages, verify the complete flow: `/api/works`, `/api/works/:workSlug/episodes`, route params, React Query keys, selected UI state, and the rendered DOM after interaction.
- If a page lists multiple works with many episodes, do not rely on vertical stacking as proof that the UI is usable. Use explicit work selection such as tabs, segmented controls, or filters so another work is visible without scrolling past a large first work.
- Avoid hardcoded `k-on` defaults in shared navigation or reusable page links unless the page is explicitly a K-ON-only entry point. When a route has a current `workSlug` and `episode`, navigation shortcuts should preserve those params.
- Frontend fallback fixtures must match production slugs closely enough for local development. If production uses `re-zero`, do not add fallback data under `rezero`; otherwise local debugging will validate the wrong path.
- For PWA issues, consider Service Worker cache separately from React state. Development mode should not keep serving stale app shells, and production service worker changes should bump the cache version or use network-first behavior for navigations.

## Linguistic Exercise Data Rules

- Keep ordinary `learning_exercises` and linguistic exercise tables separate. Linguistic training pages should read `linguistic_exercise_drafts` through the Worker/API, not by copying rows into ordinary exercise flows.
- Normalize work slug aliases at API and UI boundaries. In this project, `rezero` from RAG/local draft flows must map to production slug `re-zero`; otherwise filters split one work into two invisible buckets.
- Treat browser `localStorage` drafts as preview-only data. If a page merges local RAG drafts with server exercises, label or normalize them so they cannot hide or masquerade as published Supabase rows.
- When publishing real generated question sets, verify database status distribution first: group by `work_slug,status`, update only the intended work, then re-check counts. Do not assume `draft` rows are visible to normal users.
- Worker endpoints with optional numeric query params must distinguish “missing” from `0`. Never use `Number(value ?? '')` for optional episode filters because `Number('') === 0`; parse only when the param is present.
- Test both global and scoped API modes after changing filters: `/api/linguistic-exercises` and `/api/linguistic-exercises?workSlug=re-zero&episode=1`. A scoped endpoint passing does not prove the global training page works.
- For draft preview, service-role access stays server-side only. If the user is not the owner or the Worker lacks `SUPABASE_SERVICE_ROLE_KEY`, the UI should safely fall back to `published` or return a clear 403 for explicit draft-only requests.

## Batch AI and Cache Rules

- For page-level generation tasks, do not fan out one frontend request per item. Send one batch request containing the page's items, let the Worker/API check cache, aggregate missing items, call AI once when practical, validate results, then split and persist per item.
- Prefer the pattern used by audit/import systems: accept a single structured payload, process it server-side, and return structured per-item results. Avoid tying UI responsiveness or API quota usage to N item-level network calls.
- Before adding an "AI generate all" button, estimate request count, quota behavior, model token budget, and cache hit path. If all visible items together fit comfortably in one prompt, batch them into one AI call instead of sequential or parallel per-item calls.
- The frontend should stay thin for bulk work: one user action, one batch endpoint, compact progress/result state, and one refresh after the batch completes. The backend should own cache lookup, deduplication, validation, retries/failures, and per-item cache writes.
- Cache probes are not generation. If the UI first reads cache for each visible item, those reads must be lightweight and should not be confused with the generation path. Generation endpoints should clearly distinguish `cached`, `generated`, and `failed` counts.

## AI Gateway Rules

- Reuse the project's existing AI abstraction before adding or switching providers. In this app, model calls should go through `callAiGateway` unless the user explicitly asks for a different runtime.
- Do not bypass an existing AI Gateway path with Workers AI just because a new endpoint fails. First diagnose the actual failure: missing secret, bad header, malformed prompt, gateway response shape, cache write failure, or validation error.
- When adding a new AI-powered feature, inspect nearby endpoints that already call AI and match their model selection, auth headers, cache behavior, reasoning settings, and error handling. Diverging from established project infrastructure needs a clear reason.
- If AI Gateway auth fails in production, check Wrangler secrets (`CF_AIG_TOKEN`) and local `.dev.vars` parity before changing model providers. Secret copy/paste must strip BOM and whitespace; runtime code should defensively trim secret values before putting them into headers.
- Do not treat fallback models as harmless. Switching from the configured Gateway model to a different provider changes quality, quotas, observability, caching behavior, and debugging assumptions.

## Verification Checklist for UI Data Bugs

When fixing "data exists but UI does not show it":

1. Query the deployed Worker API endpoint and confirm the exact JSON shape, slug, and count.
2. Open the actual page in a browser automation check and inspect rendered body text after `networkidle`.
3. Capture which JS/CSS assets and API endpoints were loaded by the page.
4. Interact with the relevant control, then verify that unrelated data disappears and expected data appears.
5. Only then report the fix as complete; include the concrete visible result, not just "build passed".
