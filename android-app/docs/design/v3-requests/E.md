# E · 辞書 / 字幕 requests

## Components (candidates for ui/design)
- `DictTabs`, `EpisodeChip`, `KanaIndexRail`, `FindField` live in `screens/library/LibraryParts.kt`.
  Other screens may want the small-tabs-on-ink-rule and the 第三話 ▾ chip; promote if so.

## Data / ViewModel
- **Vocab example line + timecode.** Canvas shows `「お姉ちゃん、そろそろ起きないと。」 12:31 憂`.
  `VocabItem` has no example line or time. Workaround: `findExampleLine()` searches the episode's
  `shadowing` sentences for the headword (or stem / reading); source shows `L{sourceLineNo} {speaker}`.
  Proposed: `VocabItem.exampleJa`, `exampleStartTime`, `exampleSpeaker` from the Worker.
- **Per-line source audio for subtitles.** The subtitle dock's play button uses TTS.
  Proposed: `SubtitleLine.audioUrl` (optional) so the dock can play 原声.
- **`libraryRevealEpisodeActionsRequest`** is no longer read by the library screen (the episode
  actions now live in the どの話？ sheet). Main agent may remove the field and its increments.
- `LibraryScreen(onOpenSettings)` is unused (settings reachable from the shell); kept for the fixed signature.
