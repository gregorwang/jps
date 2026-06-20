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

## Verification Checklist for UI Data Bugs

When fixing "data exists but UI does not show it":

1. Query the deployed Worker API endpoint and confirm the exact JSON shape, slug, and count.
2. Open the actual page in a browser automation check and inspect rendered body text after `networkidle`.
3. Capture which JS/CSS assets and API endpoints were loaded by the page.
4. Interact with the relevant control, then verify that unrelated data disappears and expected data appears.
5. Only then report the fix as complete; include the concrete visible result, not just "build passed".
