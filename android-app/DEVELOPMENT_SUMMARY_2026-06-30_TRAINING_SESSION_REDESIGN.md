# 2026-06-30 Android Training Session Redesign Summary

This document summarizes the current Android app progress before starting the next task in a new session.

## Current Decision

Android should align with the web app's backend semantics, but it must not embed server-side secrets.

Accepted architecture:

- Android calls the existing Worker API.
- The Worker reads Supabase.
- The Worker calls AI Gateway.
- Supabase service role keys, AI Gateway tokens, and other private credentials stay in Worker/Wrangler secrets.
- Android must not directly call Supabase with private keys or directly call AI Gateway with private tokens.

Important clarification:

- The web training page also calls `/api/linguistic-exercises` and `/api/ai/explain`.
- The actual Supabase and AI Gateway logic is in `src/worker.ts`.
- Therefore, Android calling Worker endpoints is not inherently wrong. The goal is to keep Android aligned with the same backend contract and user flow as the web app.

## Agent Rule Added

File:

- `agent.MD`

Added global rule:

```text
DO NOT send optional commentary.
```

## Android UX Direction

The user explicitly rejected doing exercises directly on a first-level tab screen.

Required UX direction:

- First-level screens are entry/hub screens.
- Actual exercises happen in second-level full-screen training sessions.
- Read-Air and ordinary Lesson quiz should share the same interaction model.
- The target mental model is closer to Duolingo:
  - choose/start from an entry page
  - enter a focused single-question session
  - hide bottom navigation during the session
  - show progress at the top
  - answer one task at a time
  - show feedback
  - continue to the next task
  - back exits to the entry page

## Android Changes Completed

### Shared Session State

File:

- `android-app/app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`

Added:

- `TrainingSessionKind`
- `LabUiState.activeSession`
- `startReadAirSession()`
- `exitTrainingSession()`

Updated:

- `selectTab()` now exits any active training session.
- `startLesson()` now enters `TrainingSessionKind.Lesson`.
- `startTargetLesson()` now enters `TrainingSessionKind.Lesson`.
- `applySelection()` clears the active session after switching work/episode.

### Root App Routing

File:

- `android-app/app/src/main/java/com/animejapaneselab/nativeapp/ui/LabApp.kt`

Updated:

- Added `BackHandler` for active training sessions.
- Bottom navigation is hidden while a training session is active.
- Root `AnimatedContent` now switches between:
  - normal first-level tabs
  - `TrainingSessionKind.Lesson`
  - `TrainingSessionKind.ReadAir`

First-level tabs remain:

- Today
- Lesson hub
- Library
- Read-Air hub
- Review
- Settings

Second-level sessions:

- `LessonScreen`
- `ReadAirSessionScreen`

### Lesson Hub + Session

File:

- `android-app/app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/LessonScreen.kt`

Added:

- `LessonHubScreen`
- `LessonPreviewNode`

Updated:

- `LessonScreen` is now the second-level full-screen exercise session.
- Session top bar includes a back button and progress.
- Mode chips were removed from the in-session screen and kept on the hub.
- Completion screen now includes a return-to-entry action.

Result:

- The bottom `训练` tab is no longer itself the exercise-taking surface.
- It is now an entry/hub screen with mode selection, node preview, and a start button.

### Read-Air Hub + Session

File:

- `android-app/app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/ReadAirScreen.kt`

Added:

- `ReadAirSessionScreen`
- `ReadAirStartCard`
- `ReadAirSessionTopBar`
- `ReadAirSessionDock`

Updated:

- `ReadAirScreen` is now the first-level entry/hub screen.
- The entry screen still contains:
  - update button
  - status summary
  - domain filter
  - question-type filter
  - episode filter
  - start session card
- The actual prompt/options/feedback/AI explanation now live in `ReadAirSessionScreen`.

Result:

- The bottom `读空气` tab is no longer directly doing questions.
- It starts a focused second-level session.

## Verification

Command run from `android-app/`:

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Result:

```text
BUILD SUCCESSFUL
```

Debug APK path:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

## Important Git/Workspace Note

At the time of this summary:

- `android-app/` is an untracked directory from the root Git repository perspective.
- Android files changed successfully and built successfully, but they do not appear in normal `git diff --stat` because the directory is untracked.
- There are unrelated tracked modifications already present outside this Android work:
  - `scripts/import-rezero-ep62-subtitles.mjs`
  - `scripts/import-rezero-missing-subtitles.mjs`
  - `scripts/repair-rezero-sentences-from-subtitles.mjs`
  - `src/worker.ts`
  - `public/sw.js`
  - `supabase/`

Do not revert unrelated changes unless explicitly instructed.

## Known Remaining Work

### UX Polish

- The new two-level flow compiles, but no emulator/device visual pass has been run yet.
- The next session should inspect the Android UI visually.
- Verify that text does not clip on small screens.
- Verify that the session screens feel like a focused mobile training flow, not a web page port.
- Consider making Lesson and Read-Air session docks more visually consistent.

### Read-Air Session Behavior

- Current Read-Air session still uses previous/next/retry controls.
- It may need to be refined further toward Duolingo behavior:
  - no arbitrary previous navigation during normal flow
  - answer first, then continue
  - clearer correct/wrong bottom feedback state
  - optional AI explanation after answer

### Lesson Session Behavior

- Existing LessonEngine and node UI were preserved.
- This reduced risk, but the ordinary Lesson session may still need deeper UX polish:
  - stronger one-question focus
  - less visible AI coach by default
  - clearer bottom action state for each question type
  - completion/XP presentation polish

### Backend/Data Audit

- Android currently calls Worker endpoints.
- This is the intended secure boundary.
- Next audit should check whether all Android endpoints exactly match the web-side data contract:
  - works
  - episodes
  - vocab
  - grammar
  - sentences
  - progress
  - review
  - linguistic exercises
  - AI explanation payloads

### AI Gateway

- Existing Worker already contains `callAiGateway`.
- Android should continue to call `/api/ai/explain`.
- If AI behavior is wrong, debug Worker configuration/secrets and request payload shape instead of putting AI Gateway credentials into Android.

## Suggested Next Session Prompt

```text
We are continuing work in C:\Users\汪家俊\jps.

First read:
- android-app/DEVELOPMENT_SUMMARY_2026-06-29_READ_AIR.md
- android-app/DEVELOPMENT_SUMMARY_2026-06-30_TRAINING_SESSION_REDESIGN.md
- agent.MD

Current goal:
Continue polishing the Android app so both ordinary Lesson quiz and Read-Air use a Duolingo-like two-level flow:
1. first-level hub/entry screen
2. second-level focused full-screen training session

Important constraints:
- DO NOT send optional commentary.
- Android must not embed Supabase service role keys, AI Gateway tokens, or other private secrets.
- Android should call Worker APIs; Worker owns Supabase and AI Gateway access.
- Do not implement handwriting unless explicitly asked.
- Do not revert unrelated existing changes.

Start by visually/structurally auditing the current Android Compose implementation:
- LabApp.kt
- LabViewModel.kt
- LessonScreen.kt
- ReadAirScreen.kt
- TodayScreen.kt

Then improve the training session UX:
- make Lesson and Read-Air session layouts more consistent
- remove first-level exercise-taking behavior
- make answer -> feedback -> continue the primary flow
- ensure bottom navigation is hidden in sessions
- ensure back exits to the hub
- keep tests passing

Validation command:
From android-app/ run:
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug

If possible, also run an emulator or screenshot pass to verify the UI on phone-sized screens.
```
