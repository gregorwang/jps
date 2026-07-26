# Android App Agent Notes

This document applies only to the native Android project in `android-app/`. Do not treat it as guidance for the web/PWA project in the repository root.

## Start Here

- Before inspecting or changing the Android app, read [`ANDROID_ENVIRONMENT.md`](ANDROID_ENVIRONMENT.md). It records the current Windows/JDK/SDK/Gradle/emulator setup, build variants, verification commands, release guards, project map, and dirty-worktree precautions.
- Read [`ANDROID_PROJECT_GUIDE.md`](ANDROID_PROJECT_GUIDE.md) for the Android-only product boundary, runtime/data-flow map, explanation of the current code size, and guidance for locating changes.
- Treat the environment snapshot as dated context, then run the short freshness checks in that document instead of rediscovering the entire machine setup.

## Hard Scope Rules

- Treat Android as an independent product. **Do not use the repository's Web/PWA UI, state machine, learning flow, or frontend implementation as the Android behavioral or visual specification. Do not pursue Web parity.**
- When Android needs remote data, derive the transport contract from backend schema, backend tests, endpoint documentation, or verified responses. A shared backend does not make the Web frontend a source of truth.
- Do not inspect or copy Web frontend behavior unless the user explicitly asks for a specific cross-client comparison in that task.
- Do not implement, migrate, scaffold, or restore handwriting/writing-practice features in the Android app. The web handwriting pages, APIs, validation code, canvas code, and stats endpoints are explicitly out of scope for Android unless the user later gives a direct instruction that overrides this note.

## 2026-07-13 Android Product Decision

The user explicitly rejected using the Web implementation as a model for Android. This decision supersedes the older 2026-06-29 parity retrospective and all instructions derived from it.

### Android Sources of Truth

Use these in order:

1. The user's explicit Android requirements.
2. Android-owned product documentation and tests.
3. Current verified Android behavior when it has not been superseded.
4. Backend transport contracts only for remote data exchange.

Do not infer Android list limits, lesson composition, fallback order, navigation, UI states, or visual behavior from Web frontend code.

### Current Android-Owned Audio Contract

Until the user changes the Android product requirement:

- TTS order is Android/platform Japanese speech, then the configured TTS Worker/API, then a visible error. Do not add hidden providers.
- Shadowing audio selection is explicit source audio, then the Android-owned generated Re:Zero source rule when applicable, then TTS when no source is available.
- A failed source request may offer a user-triggered TTS fallback, but must not silently change provider.
- Generated URL rules, exception lists, reliability labels, and autoplay decisions are Android product logic and require Android-owned tests.

### Required Practice Going Forward

- Keep writes under `android-app/` unless the user explicitly includes a backend or another root project.
- Validate remote payloads against backend contracts and verified responses. Supporting current and legacy field names is a transport compatibility choice, not Web parity.
- Add Android-owned unit tests for lesson node types, batching, audio selection, generated URLs, payload parsing, progress semantics, and state transitions.
- Add Compose/device tests for user-visible flows that JVM tests cannot prove.
- Use emulator/device testing after source-level logic and tests are in shape. A missing device is not a blocker for source work.
- Distinguish logs proving an API was invoked from proof that a human heard audio or completed a UI flow.
- Do not keep adding unrelated responsibilities to `LabViewModel.kt`, `LessonScreen.kt`, or other giant files. Prefer a feature-owned state contract and a state-holder/UI split for new work.
