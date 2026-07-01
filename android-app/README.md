# Anime Japanese Lab Android

Native Android companion app for the existing Anime Japanese Lab project.

This app is intentionally isolated in `android-app/` so the current web/PWA project stays untouched. It is written in Kotlin with Jetpack Compose and focuses on a mobile learning path:

- Today dashboard with streak, energy, XP, and episode progress.
- Work and episode selection for K-ON! and Re:Zero instead of a fixed episode.
- Lesson flow with study cards, listening prompts, matching, single choice, cloze, tile ordering, shadowing, feedback sounds, and AI explanations.
- Vocabulary, grammar, and shadowing material views.
- Web-aligned read-air / linguistic training with `/api/linguistic-exercises`, local sample fallback, filters, answer feedback, progress sync, and AI explanation.
- Mistake book with local persistence and Worker progress sync.
- Settings for Worker API, TTS Worker, AI model, automatic speech, feedback sounds, and cloud sync.
- Pull-to-update style action that fetches the web Worker catalog, current episode materials, and read-air exercises. This is data refresh, not APK self-update.
- Web-aligned lesson tracks: mixed, vocabulary, grammar, shadowing, and review.
- No handwriting module.

## Integration

- Default Worker API: `https://anime-japanese-lab.ishallnotwant123.workers.dev`
- Progress sync uses the existing `/api/progress` and `/api/review/today` device-id protocol.
- AI explanations use the existing `/api/ai/explain` endpoint.
- Read-air exercises use the existing `/api/linguistic-exercises` endpoint and save exercise progress with the same `selected`, `answer`, `domain`, `phenomenonKey`, and `questionType` payload fields as the web training page.
- Lesson generation mirrors the web `LessonPlayer` adapter: vocab items produce study cards, pair matching, and meaning-to-Japanese choices; grammar points produce study cards, cloze, and function choices; sentences produce source/TTS audio tiles, translation tiles, and shadowing self-checks.
- Re:Zero shadowing audio uses the same CDN derivation rules as the web app, with source audio preferred and TTS fallback shown when available.
- TTS first tries Android `TextToSpeech` with `Locale.JAPAN`; if the device has no usable Japanese voice, it downloads audio from the configurable Edge TTS Worker and plays it through `MediaPlayer`. Settings also include a shortcut to install system TTS voice data.
- Feedback sounds are from Kenney UI Audio, CC0: https://kenney.nl/assets/ui-audio
- Motion is implemented with Compose animation primitives; Lottie remains a good next step for larger celebration animations.

## Build

From this folder:

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Full verification:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```
