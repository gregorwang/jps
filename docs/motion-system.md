# Android Motion System

## Current Diagnosis

- The app is Jetpack Compose based. Bottom navigation lives in `LabApp.kt`; Today, Lesson, Library, Review, Mine, Settings and subtitle flows are Compose screens.
- Answer rendering is currently in `LessonScreen.kt` through `ChoiceButton` and `LearningChoiceButton`; route nodes are rendered in the lesson hub; completion is an inline `LessonComplete`.
- Audio is split between lesson media/TTS and short feedback sounds. Lesson audio uses `MediaPlayer`; feedback currently uses a small `SoundPool` wrapper with success/error assets.
- Haptics exist for answer and completion, but they are called directly from screens instead of a single feedback event pipeline.
- Existing animations are useful but scattered: choice color/scale, dock visibility, scroll effects, and a completion pulse. There are no shared motion tokens or reduced-motion policy.

## Motion Principles

- Every tap must produce visual feedback within 100ms.
- Ordinary UI motion stays light and never competes with Japanese content.
- Answer feedback is medium strength: color, icon, sound and haptic should agree.
- Completion, unlock and streak moments are the only heavy reward animations.
- All motion must support reduced motion. Reduced motion keeps semantic state changes and disables shake, flyout and large page movement.
- Prefer transform and alpha. Avoid animating width, height, top, left or margin in hot paths.
- Lesson practice should render only the current item plus transition neighbors, not the entire queue.
- UI sound effects must not interrupt or replace Japanese source/TTS audio.

## Motion Tokens

Durations:

- `tapDown`: 70ms
- `tapUp`: 140ms
- `micro`: 160ms
- `cardEnter`: 240ms
- `pageTransition`: 280ms
- `answerFeedback`: 420ms
- `answerWrongShake`: 360ms
- `nodeUnlock`: 700ms
- `xpCount`: 900ms
- `lessonComplete`: 1600ms

Scales:

- `buttonPressedScale`: 0.97
- `optionPressedScale`: 0.98
- `popOvershootScale`: 1.12
- `nodeActiveScale`: 1.04

Easing:

- `standard`
- `decelerate`
- `springSoft`
- `springPop`
- `shake`

## Feedback Events

- `TapPrimary`: primary tap sound, light haptic.
- `TapSecondary`: light tap sound, light haptic.
- `OptionSelect`: option sound, light haptic.
- `AnswerCorrect(xp)`: correct sound, confirm haptic, mascot correct, XP flyout.
- `AnswerWrong`: wrong sound, reject haptic, mascot wrong, explanation reveal.
- `Combo(count)`: combo sound and mascot combo.
- `LessonStepComplete`: light step feedback.
- `LessonNodeUnlock`: unlock sound, light haptic, mascot unlock.
- `XpGain(amount)`: tick sound.
- `StreakExtend(days)`: streak sound, mascot streak.
- `LessonComplete`: completion sound, confirm haptic, mascot complete.
- `ReviewScheduled(count)`: soft completion feedback.

## Reduced Motion

- Press feedback remains, but uses a shorter scale transition.
- Answer wrong shake is disabled; wrong state uses color and icon only.
- Page transition becomes crossfade.
- XP flyout and mascot pop are disabled or replaced with static state.
- Count-up may still run at a short duration for readability.

## Performance Notes

- Use `graphicsLayer` for scale, alpha and translation.
- Use `AnimatedContent` with content keys for question swaps.
- Use Lazy lists with stable `key` and `contentType` for large lists.
- Do not create or preload Rive/SoundPool assets in item rows.
- Continuous 40-question practice should avoid composing hidden questions.
