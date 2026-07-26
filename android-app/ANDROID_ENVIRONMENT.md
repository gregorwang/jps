# Android Environment and Project Handoff

Last observed: **2026-07-13 (Asia/Hong_Kong)**  
Project: `AnimeJapaneseLabAndroid`  
Android project root: `C:\Users\汪家俊\jps\android-app`  
Repository root: `C:\Users\汪家俊\jps`

This is the first document a new AI session should read after `android-app/AGENTS.md`. It is a dated environment snapshot plus a working contract, not a substitute for checking the current diff.

## Five-Minute Startup Checklist

Run these before doing Android work:

```powershell
Set-Location 'C:\Users\汪家俊\jps\android-app'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

git -c safe.directory='C:/Users/汪家俊/jps' status --short
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
.\gradlew.bat --version --no-daemon
```

Then:

1. Read `AGENTS.md`, this file, and `ANDROID_PROJECT_GUIDE.md` completely.
2. Preserve the dirty worktree. Do not reset, clean, delete, or overwrite existing modified/untracked files.
3. Confirm whether the task is Android-only. Do not use the Web/PWA frontend as the Android behavior or UI reference. Read backend contracts only when remote data integration requires them; writing outside `android-app/` requires explicit user scope.
4. Use the existing emulator only after source-level logic and unit tests are in shape.

## Product Decisions and Hard Boundaries

- **Android intentionally requires login.** Do not add anonymous learning unless the user explicitly changes this decision.
- Do not implement or restore handwriting/writing-practice features in Android.
- **Android is an independent product.** The Web/PWA frontend is not the behavioral, learning-flow, navigation, visual, fallback, or UI-state reference. This 2026-07-13 decision supersedes older parity notes.
- Shared backend endpoints are transport dependencies only. Confirm their shape from backend schema/tests/docs or verified responses, not from Web frontend behavior.
- TTS order is fixed: Android/platform Japanese speech, then the configured Worker/API, then an error. There is no hidden Google Translate or other third-party fallback.
- Shadowing audio order is fixed as an Android-owned rule: explicit source audio, Android-generated Re:Zero source URL when applicable, then TTS only when source audio is unavailable. A failed source request may offer a user-triggered TTS fallback but must not silently switch providers.
- Copied/reference visual and audio assets are personal/local-use only unless the license log says otherwise. Read `..\docs\assets-license.md` before changing asset packaging or release behavior.

## Host Environment Snapshot

| Item | Observed value |
| --- | --- |
| Host | Windows NT 10.0, build `26200`; PowerShell shell |
| Android Studio | `C:\Program Files\Android\Android Studio` |
| Bundled JDK | JetBrains Runtime / OpenJDK `21.0.10` |
| JDK path | `C:\Program Files\Android\Android Studio\jbr` |
| Global `JAVA_HOME` | Not present in the observed shell; set it per command/session |
| Android SDK | `C:\Users\汪家俊\AppData\Local\Android\Sdk` |
| `local.properties` | Contains `sdk.dir`; value is intentionally not copied here |
| Installed platforms | `android-34`, `android-35`, `android-36`, `android-36.1` |
| Installed build tools | `34.0.0`, `35.0.0`, `36.1.0` |
| ADB | `1.0.41`, platform-tools `36.0.2-14143358` |
| Git repository | `C:\Users\汪家俊\jps`, current observed branch `main` |

Useful executables:

```text
Java:      C:\Program Files\Android\Android Studio\jbr\bin\java.exe
ADB:       C:\Users\汪家俊\AppData\Local\Android\Sdk\platform-tools\adb.exe
SDK root:  C:\Users\汪家俊\AppData\Local\Android\Sdk
```

Do not commit `local.properties`, keystores, passwords, session cookies, or machine-specific absolute paths into production configuration.

## Android Toolchain

| Component | Version/configuration |
| --- | --- |
| Gradle Wrapper | `8.14.5` |
| Android Gradle Plugin | `8.13.2` |
| Kotlin | `2.0.21` |
| Kotlin Compose plugin | `2.0.21` |
| Java/Kotlin target | JVM 17 bytecode, built with JBR 21 |
| `compileSdk` | 36.1 (API 36, minor API 1) |
| `targetSdk` | 36 |
| `minSdk` | 26 |
| Application ID | `com.animejapaneselab.nativeapp` |
| App version | `0.2.0` / versionCode 2 |
| ABIs | `x86_64`, `arm64-v8a` |
| UI | Jetpack Compose / Material 3 |
| Motion | Compose animation, Rive `11.7.1`, Lottie `6.7.1` |
| Local tests | JUnit 4 plus `org.json` |

Gradle is configured with a 4 GiB heap. On this machine, Kotlin compilation, Lint, and R8 can remain quiet for one to four minutes while consuming CPU. Use a realistic timeout (three to six minutes), and do not interpret buffered/no output as success or failure.

## Build Variants

| Variant | Purpose | Signing/assets |
| --- | --- | --- |
| `debug` | Development and emulator work | Debug signing; internal reference assets enabled |
| `localSlim` | Personal installable, minified build | Inherits Release minification/resource shrinking, uses debug signing, internal reference assets enabled |
| `release` | Public/distribution build only | Internal reference assets disabled at runtime; packaging is blocked unless secure signing and asset-clearance gates are supplied |

The main source set currently adds `../local-fusion-assets/res`. Those assets are not cleared for public redistribution. Public Release tasks deliberately fail unless all of the following are configured outside source control:

```text
AJL_RELEASE_STORE_FILE
AJL_RELEASE_STORE_PASSWORD
AJL_RELEASE_KEY_ALIAS
AJL_RELEASE_KEY_PASSWORD
AJL_PUBLIC_ASSETS_CLEARED=true
```

Never replace these variables with hardcoded secrets. `localSlim` is the expected personal-use deliverable until public asset clearance and a real release key exist.

## Standard Build and Verification Commands

Set Java once per PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location 'C:\Users\汪家俊\jps\android-app'
```

Fast compile and JVM regression suite:

```powershell
.\gradlew.bat testDebugUnitTest --no-daemon --console=plain --max-workers=2
```

Full Debug static analysis:

```powershell
.\gradlew.bat lintDebug --no-daemon --console=plain --max-workers=2
```

Personal minified APK:

```powershell
.\gradlew.bat assembleLocalSlim --no-daemon --console=plain --max-workers=2
```

APK output:

```text
app\build\outputs\apk\localSlim\app-localSlim.apk
```

Install and launch on the current emulator:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = 'C:\Users\汪家俊\jps\android-app\app\build\outputs\apk\localSlim\app-localSlim.apk'

& $adb install -r $apk
& $adb shell am force-stop com.animejapaneselab.nativeapp
& $adb shell monkey -p com.animejapaneselab.nativeapp -c android.intent.category.LAUNCHER 1
& $adb shell pidof com.animejapaneselab.nativeapp
```

Check startup crashes:

```powershell
& $adb logcat -c
# Launch the app, wait a few seconds, then:
& $adb logcat -d -t 500 | Select-String -Pattern 'FATAL EXCEPTION|AndroidRuntime|Process: com.animejapaneselab.nativeapp' -Context 0,8
```

`git diff --check` may print CRLF conversion warnings on this Windows checkout even when it exits successfully. Treat the exit code and actual whitespace errors separately.

## Last Known Emulator Configuration

| Item | Observed value |
| --- | --- |
| Serial | `emulator-5554` |
| State | Stopped after the 2026-07-13 smoke test; values below are the last verified configuration |
| Model/product | `sdk_gphone64_x86_64` / `sdk_gphone64_x86_64` |
| Android | Android 16 / API 36 |
| ABI | `x86_64` |
| Resolution | `1080x2400` |
| Density | `420` |
| Window animation scale | `0` |
| Transition animation scale | `0` |
| Animator duration scale | `0` |

The last known emulator animation scales were zero. When this emulator is connected, it is suitable for functional smoke tests but **not** for validating motion quality, durations, Lottie/Rive behavior, or reduced-motion fallbacks. For motion testing, temporarily set all three scales to `1`, record that change in the test report, and restore the user's preferred values afterwards.

An emulator log proving that TTS was invoked is not proof that a human heard audio. Verify host/emulator audio routing before describing playback as audible.

## Project Map

```text
android-app/
  AGENTS.md                       Android-specific agent rules
  ANDROID_ENVIRONMENT.md          this handoff
  ANDROID_PROJECT_GUIDE.md        Android architecture, runtime/data flow, code-size explanation
  app/build.gradle.kts            variants, dependencies, signing/release gates
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/animejapaneselab/nativeapp/
    MainActivity.kt               launcher; AndroidX SplashScreen entry
    ComposeHost.kt                installs the Compose root
    LabApplication.kt             application initialization
    data/                          models, local store, remote client, repository
    domain/                        lesson engine, progress and review planning
    platform/                      Android/Xiaomi device capability diagnostics
    ui/
      LabApp.kt                   root navigation/composition
      LabViewModel.kt             main state holder and orchestration
      audio/                       source audio, platform TTS, Worker TTS, recording
      completion/                 completion UI/state mapping
      components/                 reusable Compose components
      feedback/                   sound/haptic/visual feedback contracts
      fusion/                     internal visual rollout/resolution
      motion/                     motion tokens and transition helpers
      rive/                       Rive/Lottie runtime hosts
      screens/                    feature screens
      theme/                      colors, typography and shapes
    update/                        app-owned update state, download, verification, and installer handoff
  app/src/test/                   JVM tests
  app/src/debug/                  debug-only manifest/preview activity
  local-fusion-assets/res/        local/internal reference resources
```

There are currently 24 JVM `*Test.kt` files and no `app/src/androidTest` suite. Prefer pure JVM tests for business rules and parsing. Add device/Compose instrumentation tests only when platform behavior genuinely needs them.

## Runtime Architecture and Persistence

- `LabViewModel` owns the main `StateFlow<LabUiState>` and coordinates auth, remote refresh, lessons, review, pronunciation, and local/cloud progress.
- Active lesson/read-air sessions map to a shared `LearningSessionStatus`. Android 16 uses `Notification.ProgressStyle`; API 36.1 also requests promoted ongoing treatment and a short status-bar chip. The notification is removed on exit and respects user dismissal for the current session.
- `SampleLearningRepository` builds local/fallback content and lesson nodes; remote payloads are adapted into the same models.
- `RemoteLabClient` uses `HttpURLConnection`; keep disconnect/finally behavior and rethrow coroutine cancellation.
- `LocalLabStore` uses SharedPreferences named `anime-japanese-lab-native`.
- Stored values include device ID, settings, session cookie, selected work/episode, last episode per work, mistakes, local progress, and pending progress sync.
- Android backup is disabled, and both backup-rule XML files exclude all SharedPreferences. Do not weaken this while auth cookies remain in SharedPreferences.
- Correct progress is committed locally before cloud upload. Cloud failure must leave an item in the pending queue for later retry.
- `cloudSync=false` means local progress still works; re-enabling sync queues local progress for upload.
- The settings-screen updater owns a separate `StateFlow`, SharedPreferences download record, and `DownloadManager` task. It verifies size, SHA-256, package name, version code, and installed-app signer before handing a narrow `content://` URI to a system installer.

## Network and Service Defaults

Default public endpoints are code defaults, not secrets:

```text
API: https://anime-japanese-lab.ishallnotwant123.workers.dev
TTS: https://cloudflare-edge-tts.ishallnotwant123.workers.dev
Update: https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev
AI model: gemini-3.1-flash-lite
Reasoning effort: high
```

The network security config permits cleartext only for emulator/local development hosts:

```text
10.0.2.2
127.0.0.1
localhost
```

Do not print session cookies, passwords, authorization headers, release passwords, or keystore contents in logs or handoff documents.

## Authentication and Audio Contracts

- The Android root UI gates learning behind account login by deliberate product choice.
- Login passwords are Compose-memory-only and are not saveable across Activity recreation.
- Session cookies are persisted locally for auth continuity but excluded from backup.
- Source audio failure is reported. A second explicit user action may switch to TTS when a fallback text exists.
- Platform Japanese `TextToSpeech` is attempted before the configured Worker. Worker audio cache keys include endpoint, voice, and text; downloads use a temporary file before final rename.
- Pronunciation recording is 16 kHz WAV and uses short-lived ticket/evaluation APIs. Assessment failure must always leave a self-assessment route so a lesson cannot dead-end.

## Assets, Rive, Lottie, and Debug Preview

- Read `..\docs\assets-license.md` and `..\docs\TARGET-ASSET-WHITELIST.json` before adding or redistributing assets.
- Internal assets are enabled in `debug` and `localSlim`, and disabled through `BuildConfig.ALLOW_INTERNAL_REFERENCE_ASSETS` in `release`.
- `app/src/debug/AndroidManifest.xml` exposes `debug.FusionPreviewActivity` only in the Debug source set.
- Reduced-motion/rich-animation switches must provide a static Compose/vector fallback rather than leaving empty UI.
- Resource-name lookup is used by the dynamic feedback asset registry; this currently accounts for one non-blocking Lint `DiscouragedApi` warning.

## Last Known Verification Baseline

Observed after the 2026-07-26 search/profile/SRS pass (RAG subtitle search `SearchScreen`, character language profiles, SRS-weighted review planning with due buckets, mistake→source-line jump — see `ANDROID_PRODUCT_ROADMAP.md` "已完成"; also same-day reading/explanation pass: furigana annotation, linguistic card payloads, sentence deep-dive, AI history screen; new `ui/reading/` package, `data/FuriganaCache.kt`, `ui/screens/AiHistoryScreen.kt`, `ui/screens/SearchScreen.kt`):

- Third same-day pass added: JLPT vocab filtering, episode plan card, change-password, online AI model list, real XP (`learningXp`), and a dead-code sweep (MineScreen.kt deleted; ~2,142 unreferenced lines removed from the four big screens).
- 2026-07-27 pass added: offline content cache (`data/EpisodeContentCache.kt`, IOException-only fallback for catalog/content GETs), exercise-type mapping repairs (`databaseDistractorPool`, explicit `reading_air_tone` exclusion), client-side subtitle scene grouping (collapsible, focus-jump aware), dead-code round 3 (−148 lines).
- `testDebugUnitTest`: **BUILD SUCCESSFUL, all tests green** (29 test files; + `EpisodeContentCacheTest`, `ExerciseTypeMappingTest`).
- `lintDebug`: **0 errors** (report at `app/build/reports/lint-results-debug.html`).
- `assembleLocalSlim`: successful incl. R8, resource shrinking, Lint Vital. APK `app\build\outputs\apk\localSlim\app-localSlim.apk`, 23,440,661 bytes, SHA-256 `35AA72FC913CB850A79F1C0A616270EC6F0C8AC097FCAF7B352FA3F3C6CC2721`.
- Known pre-existing Kotlin deprecation warnings: `LocalClipboardManager` in LessonScreen/ReadAirScreen/SubtitleBrowserScreen.
- Emulator smoke test not re-run in this pass (source-level + build verification only). Same for the earlier same-day UI/design-system overhaul (theme rework, per-screen refresh, `LabTheme.colors` semantic tokens).

Previous baseline, observed after the 2026-07-13 data-flow, progress-persistence, and app-update pass:

- `testDebugUnitTest --rerun-tasks`: **148 tests, 0 failures, 0 errors, 0 skipped** across 24 test files.
- `lintDebug`: **0 errors/fatals, 20 warnings, 2 hints**.
- Warning categories: dependency update suggestions, personal-use GIF format warnings, three Compose modifier-parameter style warnings, two KTX suggestions, two autoboxing hints, one dynamic-resource lookup warning, and one unused legacy K-ON asset.
- `assembleLocalSlim`: successful with Kotlin compile, R8, resource shrinking, Lint Vital, APK packaging, and signing validation.
- Update Worker: 5 Vitest tests passed, TypeScript typecheck passed, Wrangler deploy passed. Deployed version `a70eb72c-6fa7-4a9f-8add-2a4191714013` uses private R2 bucket `anime-japanese-lab-android-updates`.
- Published release `0.2.0` / versionCode 2: latest-manifest GET, APK HEAD, byte Range, full download, and remote/local SHA-256 equality all verified through the production Worker.
- API 36.1 emulator: APK install succeeded; Android reported versionCode 2/versionName 0.2.0; `MainActivity` cold launch completed, process remained alive and resumed, and the app PID had no `FATAL EXCEPTION`, app ANR, `AndroidRuntime`, or `SecurityException` match. The UI correctly stopped at the intentional login gate, so the authenticated settings-button and OEM installer click flow were not fabricated as tested.
- Last observed APK: `app\build\outputs\apk\localSlim\app-localSlim.apk`, 23,309,589 bytes.
- Last observed APK SHA-256: `D2B551CA6D30BE62E993F2A35221E310FD21A4D5D548AF3C5A6A01B12853BEC3`.
- `localSlim` is signed with the Android debug certificate and is for personal/local installation, not public release.

This baseline becomes stale as soon as source, dependencies, assets, SDK, or emulator settings change. Re-run the relevant command and update this section rather than claiming the old result.

## Dirty Worktree Warning

At the time of this snapshot, the repository had extensive existing modified, deleted, and untracked files across the Android app and repository root. These changes belong to the user unless proven otherwise.

Rules for every future session:

- Never run `git reset --hard`, `git clean`, or broad checkout/restore commands.
- Never delete untracked assets, screenshots, verification folders, local APIs, or source files just because Git reports them as untracked.
- Inspect focused diffs for files you need to touch.
- Use `git -c safe.directory='C:/Users/汪家俊/jps' ...` because Git may otherwise reject the workspace ownership.
- Keep Android writes under `android-app/` unless the user explicitly includes the web Worker, pronunciation API, Supabase, or other root projects.
- Generated folders such as `.kotlin/`, `build/`, large reference packs, verification output, and log files are ignored where appropriate; do not assume ignored files are disposable.

## Known Maintenance Notes

- `app/proguard-rules.pro` still contains comments/keep rules referring to the removed custom `SplashActivity`. They are currently harmless but should be reviewed before future startup/R8 cleanup rather than copied as current architecture.
- Dependency-update Lint warnings are advisory. Upgrade AGP/Kotlin/Compose/AndroidX as a deliberate migration with tests, not as an incidental cleanup during an unrelated feature.
- Release packaging is intentionally guarded. A failure asking for signing variables or public asset clearance is expected, not a Gradle defect.
- The current app uses SharedPreferences rather than Room. Preserve atomic local-first progress semantics if storage is migrated.

## Updating This Handoff

Update this file when any of these change:

- JDK, Android Studio, SDK platforms/build tools, Gradle, AGP, Kotlin, Compose, Rive, or Lottie versions.
- Emulator serial/API/resolution/density/animation scales.
- Build variants, signing variable names, release gates, application ID, or ABI list.
- Default endpoints, auth contract, source/TTS order, persistence backend, or backup behavior.
- Test suite size, standard verification commands, or last known baseline.
- Asset licensing/packaging boundary.

Do not add secrets. Record variable names and locations, not values.

## Suggested Prompt for a New AI Session

```text
请先完整阅读 C:\Users\汪家俊\jps\android-app\AGENTS.md 和
C:\Users\汪家俊\jps\android-app\ANDROID_ENVIRONMENT.md，以及
C:\Users\汪家俊\jps\android-app\ANDROID_PROJECT_GUIDE.md。
Android 是独立产品，不要把 Web/PWA 前端作为行为、学习流程或 UI 规范。
然后只做文档里的 Five-Minute Startup Checklist 和与本次任务直接相关的增量检查。
保留当前脏工作区，不要 reset/clean，不要重新从头探测整台机器。
```
