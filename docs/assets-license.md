# Assets License Log

## Included Assets

| Asset | Source | License | Modified | Usage |
| --- | --- | --- | --- | --- |
| `feedback_success.ogg` | Kenney UI Audio, bundled in project | See `android-app/app/src/main/res/raw/kenney_ui_audio_license.txt` | No | UI success / completion fallback |
| `feedback_error.ogg` | Kenney UI Audio, bundled in project | See `android-app/app/src/main/res/raw/kenney_ui_audio_license.txt` | No | UI wrong-answer fallback |
| `k_on_*_character*.gif` (Yui, Mio, Ritsu, Mugi, Azusa, Nodoka, Ui, Sawako, Jun) | [TBS K-ON! official character page](https://www.tbs.co.jp/anime/k-on/k-on_tv/chara/chara.html) | ©かきふらい・芳文社／桜高軽音部; personal/local use only, no public redistribution clearance recorded | No | K-ON! course-aware characters for Today, exercise lab, lesson prompts, path and completion. `k_on_azusa_character.gif` is the legacy Nodoka image; the corrected Azusa asset is `k_on_azusa_character_v2.gif`. |
| `rezero_*_character.jpg` (Subaru, Emilia, Puck, Ram, Rem, Beatrice, Otto, Frederica, Echidna, Petelgeuse) | [Re:Zero official character page](https://re-zero.com/character/) | Official site artwork; personal/local use only, no public redistribution clearance recorded | No | Re:Zero course-aware characters for Today, exercise lab, linguistic training, path and completion |

## Reference-Only Material

| Reference | Source | Usage | Asset Copy |
| --- | --- | --- | --- |
| Mirror App fusion visual pack | Local `proui/mirror-app` workspace | Personal/local learning UI: Today companion, lesson companion, speaker, feedback icons, path visuals and completion celebration | Yes; byte hashes are recorded in `TARGET-ASSET-WHITELIST.json` |
| Mirror App lesson Rive pack | Local `proui/mirror-app` workspace | State-driven progress, combo delight and correct-answer CTA lightning | Yes; internal-reference-only |
| Mirror Junior in-lesson pack | Local Mirror raw/drawable resources | Translation word-bank coach with idle/correct/incorrect reactions | Yes; internal-reference-only |
| Mirror Falstaff Duo Radio pack | Local Mirror/Duolingo-like raw/drawable resources | Listening-match host stage and four waveform cards | Yes; internal-reference-only |
| Mirror App answer sounds | Local `proui/mirror-app` workspace | One-shot correct/incorrect feedback | Yes; internal-reference-only |
| Duolingo interaction rhythm | Public product observation plus Mirror evidence | Timing, direction, hierarchy and feedback-order reference | Behavior is reimplemented in target-owned Kotlin |

## Internal-Use Boundary

- The user authorized these copied assets for personal, local use. No public distribution clearance is recorded.
- Mirror lesson grading, queues, navigation, mock payloads and retry business rules were not copied; target-owned lesson state drives every imported visual and sound.
- Reduced-motion and Compose/vector fallbacks remain target-owned so the learning flow stays usable when rich animation is disabled.
- Any additional Rive, Lottie, image or sound must be added to this file and `TARGET-ASSET-WHITELIST.json` before use.
