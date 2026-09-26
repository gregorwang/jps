# Assets License Log

## Included Assets

| Asset | Source | License | Modified | Usage |
| --- | --- | --- | --- | --- |
| `feedback_success.ogg` | Kenney UI Audio, bundled in project | See `android-app/app/src/main/res/raw/kenney_ui_audio_license.txt` | No | UI success / completion fallback |
| `feedback_error.ogg` | Kenney UI Audio, bundled in project | See `android-app/app/src/main/res/raw/kenney_ui_audio_license.txt` | No | UI wrong-answer fallback |
| IBM Plex Mono (`ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf`) | [IBM Plex](https://github.com/IBM/plex), bundled in `android-app/app/src/main/res/font/` | SIL Open Font License 1.1; full text in `android-app/third_party/fonts/IBMPlexMono-OFL.txt` | No | v3 mono labels (timecodes, counters, hints) |
| `k_on_*_character*.gif` (Yui, Mio, Ritsu, Mugi, Azusa, Nodoka, Ui, Sawako, Jun) | [TBS K-ON! official character page](https://www.tbs.co.jp/anime/k-on/k-on_tv/chara/chara.html) | ©かきふらい・芳文社／桜高軽音部; personal/local use only, no public redistribution clearance recorded | No | K-ON! course-aware characters for Today, exercise lab, lesson prompts, path and completion. `k_on_azusa_character.gif` is the legacy Nodoka image; the corrected Azusa asset is `k_on_azusa_character_v2.gif`. |
| `rezero_*_character.jpg` (Subaru, Emilia, Puck, Ram, Rem, Beatrice, Otto, Frederica, Echidna, Petelgeuse) | [Re:Zero official character page](https://re-zero.com/character/) | Official site artwork; personal/local use only, no public redistribution clearance recorded | No | Re:Zero course-aware characters for Today, exercise lab, linguistic training, path and completion |

## Reference-Only Material

Since v3 (app 0.4.0) every row below has been **removed from the App**; the copied files are archived (not deleted) under `archive-content-sources/local-fusion-assets/` and `archive-content-sources/android-drawables/`. This includes the DuolingoSans font, all Rive (`.riv`) and Lottie (`.json`) animations, the `.hla` haptic patterns and the Duolingo-like sounds. The `rive-android` and `lottie-compose` dependencies were dropped as well.

| Reference | Source | Usage | Asset Copy |
| --- | --- | --- | --- |
| Mirror App fusion visual pack | Local `proui/mirror-app` workspace | v2 only (Today/lesson companion, speaker, feedback icons, path visuals, completion celebration, DuolingoSans font) | v3 起已移出 App、素材已归档 |
| Mirror App lesson Rive pack | Local `proui/mirror-app` workspace | v2 only (Rive progress, combo, CTA lightning) | v3 起已移出 App、素材已归档 |
| Mirror Junior in-lesson pack | Local Mirror raw/drawable resources | v2 only (Rive/Lottie word-bank coach) | v3 起已移出 App、素材已归档 |
| Mirror Falstaff Duo Radio pack | Local Mirror/Duolingo-like raw/drawable resources | v2 only (listening host, waveform vectors) | v3 起已移出 App、素材已归档 |
| Mirror App answer sounds | Local `proui/mirror-app` workspace | v2 only (answer sounds and `.hla` haptics); v3 uses the Kenney sounds above and platform haptic constants | v3 起已移出 App、素材已归档 |
| Duolingo interaction rhythm | Public product observation plus Mirror evidence | v2 only | v3 起已移出 App（v3 motion follows `android-app/design/MOTION_SPEC.md`） |

## Internal-Use Boundary

- The user authorized these copied assets for personal, local use. No public distribution clearance is recorded.
- Mirror lesson grading, queues, navigation, mock payloads and retry business rules were not copied; target-owned lesson state drives every imported visual and sound.
- Reduced-motion and Compose/vector fallbacks remain target-owned so the learning flow stays usable when rich animation is disabled.
- Any additional image, font or sound must be added to this file and `TARGET-ASSET-WHITELIST.json` before use.
