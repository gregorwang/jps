# 2026-06-29 Read-Air Android Parity Summary

This document summarizes the Android-only work completed in this round. The root web/PWA project was used only as a reference source and was not modified.

## Packaging Status

- Debug APK has been built successfully.
- APK path: `android-app/app/build/outputs/apk/debug/app-debug.apk`
- Latest verified build command:

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

- Result: `BUILD SUCCESSFUL`

## Update Logic Clarification

The Android app currently has data pull/update logic, not APK self-update logic.

Existing in-app update behavior:

- Today page and Settings page call `LabViewModel.refreshFromServer()`.
- Read-air page can call `LabViewModel.refreshReadAirExercises()`.
- These methods pull Worker data through `RemoteLabClient`.
- They refresh works, episodes, vocab, grammar, shadowing sentences, read-air exercises, and progress/review data depending on the action.

What does not exist yet:

- No APK version check.
- No APK download.
- No in-app install prompt.
- No Play Store / internal app update integration.

If APK self-update is needed later, implement it as a separate explicit feature. Do not confuse it with the existing Worker data refresh.

## Web Reference Used

Main web files inspected for parity:

- `src/routes/LinguisticTrainingPage.tsx`
- `src/routes/EpisodeLinguisticsPage.tsx`
- `src/routes/RagPage.tsx`
- `src/worker.ts`
- `src/server/repositories/animeRepository.ts`
- `src/lib/types.ts`

Relevant web behavior copied:

- `/api/linguistic-exercises` is the source of published linguistic/read-air exercises.
- Answer progress uses item type `exercise`.
- Progress payload includes `label`, `selected`, `answer`, `domain`, `phenomenonKey`, and `questionType`.
- Single-question training keeps the just-answered exercise pinned until the user moves next, then removes answered items from the active queue.
- AI explanation uses `/api/ai/explain` with `kind: "linguistic"` and a context built from the exercise, selected answer, correct answer, domain, phenomenon, prompt, options, and explanations.

## Android Changes

### Data models

File: `app/src/main/java/com/animejapaneselab/nativeapp/data/LearningModels.kt`

Added:

- `LinguisticExercise`
- `LinguisticSceneLine`
- `LinguisticExerciseOption`
- `LinguisticExerciseAnswer`

Also added `payload` to `ProgressItem` so synced web progress can restore selected answers.

### Remote API parsing

File: `app/src/main/java/com/animejapaneselab/nativeapp/data/RemoteLabClient.kt`

Added:

- `fetchLinguisticExercises(selection)`
- `parseLinguisticExercisesJson`
- `buildLinguisticProgressPayload`

Parsing supports both camelCase and snake_case fields where backend variants may appear.

Supported exercise fields include:

- `id`
- `batchId`
- `workSlug`
- `episode`
- `sourceId`
- `sourceLineNo`
- `jaText`
- `zhText`
- `sceneLines`
- `targetLineNo`
- `domain`
- `phenomenonKey`
- `questionType`
- `prompt`
- `options`
- `optionItems`
- `answer`
- `hint`
- `basicExplanationZh`
- `deepExplanationZh`
- `animeContextNoteZh`
- `cautionNoteZh`
- `difficulty`
- `qualityScore`
- `status`
- phenomenon metadata

### Local fallback

File: `app/src/main/java/com/animejapaneselab/nativeapp/data/SampleLearningRepository.kt`

Added `readAirExercises(selection)` to convert the previous local read-air sample scenes into `LinguisticExercise` objects.

This keeps fallback behavior on the same training contract as remote web exercises instead of returning to the old free-question preview.

### ViewModel/state

File: `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`

Added:

- `ReadAirTrainingState`
- `ReadAirFilters`
- read-air exercise refresh
- domain/question-type/episode filters
- answer selection
- pinned answered exercise queue behavior
- retry/current/previous/next controls
- read-air AI explanation
- read-air mistake insertion
- web-compatible exercise progress payload sync
- synced progress restoration from payload `selected`

`refreshFromServer()` now also pulls read-air exercises for the selected episode.

### UI

Files:

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabApp.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/ReadAirScreen.kt`

The old read-air preview was replaced with a training UI:

- status summary
- data refresh button
- domain filter
- question type filter
- episode filter when applicable
- prompt and dialogue display
- option buttons
- correctness feedback
- explanation details
- previous/next/retry controls
- AI explanation button

Labels match web intent, including:

- `kuuki_yomi` -> `读空气`
- `pragmatics` -> `语用学`

### Documentation

Files:

- `README.md`
- `AGENTS.md`
- this document

`AGENTS.md` already contains the hard rule that handwriting/writing-practice features must not be implemented in Android unless the user explicitly overrides it.

## Tests Added

Files:

- `app/src/test/java/com/animejapaneselab/nativeapp/data/RemoteLabClientTest.kt`
- `app/src/test/java/com/animejapaneselab/nativeapp/data/SampleLearningRepositoryTest.kt`
- `app/src/test/java/com/animejapaneselab/nativeapp/ui/ReadAirTrainingStateTest.kt`

Covered:

- remote linguistic exercise parsing
- option object parsing
- string option parsing
- correct key / correct index answer resolution
- web-compatible progress payload fields
- local read-air fallback exercise contract
- answered exercise pinned/unpinned queue behavior

## Verification

Command run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Result:

- Unit tests passed.
- Debug APK assembled.
- No emulator/device test was run in this round.

## Known Remaining Gaps

- No APK self-update flow exists.
- Read-air RAG question generation/saving from Android is not implemented. Android currently consumes existing `/api/linguistic-exercises`; generation remains on web/backend.
- Structured AI response rendering is simpler than the web `StructuredAiResultView`; Android currently displays returned text/sections as plain text.
- Broader linguistic browse mode from the web exists only as a training-focused Android screen right now.
- Handwriting remains explicitly out of scope.

## Next Recommended Steps

1. Add an Android screen/state for episode-level linguistic browse mode if parity requires browsing all questions, not only training.
2. Improve AI explanation rendering to match the web structured sections more closely.
3. Decide whether APK self-update is actually needed. If yes, design it separately from Worker data refresh.
4. Continue the broader parity audit for subtitles/deep-dive, card-level linguistic payload display, character/profile/history/account features, and remaining AI settings.
