# Letter-answer exercises batches 8–10 cross-review

## Scope and evidence

- Reviewed `batch8_exercises_letter.json`, `batch9_exercises_letter.json`, and `batch10_exercises_letter.json`: 120 regenerated EP17 exercises.
- Matched every ID to its original one-letter-answer row in `learning_exercises`.
- Checked every cited vocabulary or grammar row against `learning_vocab_items` and `learning_grammar_points`.
- Read the authoritative Japanese `subtitle_lines` around every occurrence. The shifted Chinese subtitle field was not used as evidence.
- Because the database subtitle table has no speaker column, ambiguous turn boundaries were also checked against the speaker-labelled Crunchyroll transcript for EP17.
- Checked source mapping, reference and speaker resolution, honorific or benefactive direction where applicable, prompt self-containment, answer accuracy, and hint leakage.
- This review made no database writes.

## Verdict

| File | Rows | Passed unchanged | Corrected | Rejected |
|---|---:|---:|---:|---:|
| `batch8_exercises_letter.json` | 40 | 31 | 9 | 0 |
| `batch9_exercises_letter.json` | 40 | 29 | 11 | 0 |
| `batch10_exercises_letter.json` | 40 | 33 | 7 | 0 |
| **Total** | **120** | **93** | **27** | **0** |

All corrected rows pass after revision.

## Substantive corrections

- `re-zero-s01e17-exercise-108`
  - Corrected the omitted subject of `今は幸いにも逃げきったはず`.
  - It is Subaru and Otto's carriage party that has temporarily escaped; the sentence does not claim that Rem escaped.
- `re-zero-s01e17-exercise-114`
  - Consequently corrected what `幸いにも` evaluates: their escape obtained through Rem's rearguard sacrifice, not Rem's sacrifice itself.
- `re-zero-s01e17-exercise-150`
  - Corrected the speaker of the first `死にたくない` from Otto to Subaru after Otto throws him from the carriage.
- `re-zero-s01e17-exercise-152`
  - Corrected the speaker and emotional progression of `嫌だ…死にたくない…` from Otto to Subaru.
- `re-zero-s01e17-exercise-160`
  - Corrected the speaker of `分からないんですか？` from Rem to Otto.

## Prompt and hint quality corrections

- Added enough scene context for the prompt to stand alone:
  - `re-zero-s01e17-exercise-050`
  - `re-zero-s01e17-exercise-070`
  - `re-zero-s01e17-exercise-080`
  - `re-zero-s01e17-exercise-118`
  - `re-zero-s01e17-exercise-122`
  - `re-zero-s01e17-exercise-124`
  - `re-zero-s01e17-exercise-136`
  - `re-zero-s01e17-exercise-144`
- Removed direct reading answers from hints while retaining derivational or mnemonic guidance:
  - `re-zero-s01e17-exercise-049`
  - `re-zero-s01e17-exercise-057`
  - `re-zero-s01e17-exercise-059`
  - `re-zero-s01e17-exercise-071`
  - `re-zero-s01e17-exercise-077`
  - `re-zero-s01e17-exercise-085`
  - `re-zero-s01e17-exercise-091`
  - `re-zero-s01e17-exercise-097`
  - `re-zero-s01e17-exercise-099`
  - `re-zero-s01e17-exercise-103`
  - `re-zero-s01e17-exercise-111`
  - `re-zero-s01e17-exercise-135`
  - `re-zero-s01e17-exercise-139`
- `re-zero-s01e17-exercise-047`
  - Clarified that the base reading of `種` is `たね` and that the realized form in `物種` is `だね` after rendaku.

## Source and coverage checks

- The files contain exactly the original 120 one-letter-answer records from `re-zero-s01e17-exercise-044` through `re-zero-s01e17-exercise-167`; the absent numeric IDs in the range are absent from the database selection as well.
- Ordered candidate-ID MD5: `8e19269a87102a183f406a2d2e4e8858`.
- Ordered database-ID MD5 for the same one-letter-answer set: `8e19269a87102a183f406a2d2e4e8858`.
- All cited EP17 vocabulary IDs and grammar IDs `grammar-001` through `grammar-011` exist and match the Japanese expressions used in the exercises.
- Speaker-sensitive sequences were checked continuously, particularly:
  - Otto's `分からないんですか` through `白鯨です`.
  - Otto's `僕ら` followed by `今は幸いにも逃げきったはず`.
  - Otto's demand that Subaru die, followed later by Subaru's repeated `死にたくない`.

## Final QA

- Validator: 120 rows, 120 unique IDs, 0 errors, 0 warnings.
- Exact five-field schema (`id`, `prompt`, `answer`, `hint`, `review_note`): 120/120.
- Blank required fields: 0.
- Duplicate normalized prompts: 0.
- Duplicate normalized prompt-answer pairs: 0.
- Seven identical-answer groups remain across 16 reading questions. These are intentional repeated readings of the same word in different subtitle occurrences, not duplicated exercises.
- Remaining one-letter answers: 0.
- Template-answer markers: 0.
- Exact reading answers repeated in their hint: 0.
- Normalized hint-answer shared spans of 8 or more characters: 0.
- Rejected rows: 0.
