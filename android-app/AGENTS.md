# Android App Agent Notes

This document applies only to the native Android project in `android-app/`. Do not treat it as guidance for the web/PWA project in the repository root.

## Hard Scope Rules

- Do not implement, migrate, scaffold, or restore handwriting/writing-practice features in the Android app. The web handwriting pages, APIs, validation code, canvas code, and stats endpoints are explicitly out of scope for Android unless the user later gives a direct instruction that overrides this note.

## 2026-06-29 Session Retrospective

### Mistakes Made

- Treated runtime probing as the main path too early. The user already had a working web implementation, so the first source of truth should have been the web project logic and API contract, with emulator/device checks only after the Android port matched that behavior.
- Added Google Translate TTS as an automatic fallback. That is not web parity. The web behavior is local browser speech first, then the configured TTS API/Worker. If the Worker returns an error, report that endpoint problem instead of silently changing the product behavior to a different provider.
- Did not prioritize original/source audio enough for Re:Zero shadowing. The web app already has source audio URLs, including derived CDN rules. Android shadowing should prefer explicit `audioUrl`/`storagePath`, then use the same Re:Zero derivation rules, and only then fall back to TTS.
- Underestimated rule-generated media URLs. Some source audio links are not directly stored as full URLs; they are generated from work, season, episode, sentence id, and known exception lists. Those rules are part of the feature contract and must be ported, not rediscovered through app runtime behavior.
- Let device/emulator availability create noise. Failing to find a connected phone is not a blocker for copying web logic into Android. Device testing is useful for final proof, but it should not drive the implementation strategy.
- Mixed "resilience fallback" with "same behavior as web". Extra fallbacks can be useful, but only when explicitly labeled, configurable, and off by default if the goal is strict parity.
- Verified "audio path invoked" more strongly than "audible output heard". Emulator logs can prove Android TTS was called, but if the emulator was launched with no host audio, the result must be described as a runtime signal rather than human-audible verification.

### Required Practice Going Forward

- For Android migration tasks, inspect the existing web code first and treat it as the behavioral spec. Identify the exact source files, data fields, endpoint shapes, URL derivation rules, fallback order, and UI states before changing Kotlin code.
- Keep the project boundary clear. Read root web files when needed as reference, but write Android changes only under `android-app/` unless the user explicitly asks for web/backend changes.
- Preserve provider parity. For TTS, mirror the web order unless the user asks otherwise:
  1. Platform/local speech.
  2. Configured TTS Worker/API.
  3. No hidden third-party fallback.
- Preserve source audio parity. For shadowing sentences:
  1. Use explicit source audio fields if present.
  2. Use web-matching generated CDN rules for Re:Zero.
  3. Mark reliability and autoplay behavior the same way as the web rules.
  4. Fall back to TTS only when no source audio is available.
- Validate API shape before adding Android model limits. Do not cap vocab, grammar, or sentence lists unless the web behavior does. Parse both current camelCase fields and legacy snake_case fields when the backend may return both.
- Add source-parity unit tests before relying on emulator checks. Tests should lock down lesson node types, batch sizes, source-vs-TTS selection, generated Re:Zero URLs, exception lists, and remote payload parsing.
- Use emulator/device testing as the last step. The purpose is to catch Android runtime integration issues after the web-aligned behavior is already implemented and tested.
- Be explicit in status updates. When a behavior is copied from web, say which file or rule it came from. When a behavior is an Android-only compromise, label it as such and explain the tradeoff before shipping it.
