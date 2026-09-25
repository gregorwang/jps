# Letter-answer exercise regeneration QA: batches 35–37

## Scope and generation method

- Frozen selection: the first 120 current single-letter-answer exercises ordered by
  `episode, sort_order, id`.
- Selection bounds: `re-zero-s01e21-exercise-184` through
  `re-zero-s01e22-exercise-064`.
- Episode split: EP21 56 rows, EP22 64 rows.
- Ordered comma-MD5: `eedf6b899655ddd940abeaa95b17ad29`.
- The teaching content was authored row by row by collaborating agents after reading
  the real subtitles and relevant sentence, grammar, or vocabulary records.
- No loop, Python template, JavaScript template, or SQL template generated
  `prompt`, `answer`, `hint`, or `review_note`. Scripts were used only for selection,
  merging the review overlay, and QA.
- This task performed no database writes.

## Candidate files

| File | Rows | Coverage | SHA256 |
|---|---:|---|---|
| `regen/batch35_exercises_letter.json` | 40 | EP21 exercises 184–214 and 216–224 | `6CA3CF0B10B070484D034DF9C1E9C27E21A44AC94EFED365FB41FCA876531A26` |
| `regen/batch36_exercises_letter.json` | 40 | EP21 exercises 225–240; EP22 exercises 001–024 | `967934958D5152C9D5C9669D78BDE6F75CDF1D20E0CCB5D0207D26CE1C91D868` |
| `regen/batch37_exercises_letter.json` | 40 | EP22 exercises 025–064 | `B5F5C84907C063FB4D41865D25A55AEAD893419008DF4F55EF4A3269189D969A` |
| `regen/generation_overlay_exercises_letter_batches35_37.json` | 2 | Replacements for exercises 235 and 236 | `D2A549C0E49BC752374C36221C56E5600B5CC401BAB04F25E807CE4AF2BFDDBC` |

The overlay is a replacement set, not an append set. Consumers must replace the
matching base objects by `id`; the effective candidate remains 120 rows. The
SHA256 of the pretty-printed merged QA artifact was
`80E62A1E7EDAE941E7357B79085C0134172A0A080BAA17B115776F3672240477`.

The two overlay entries make wording-only corrections so that the learner-facing
answers do not contain the validator's reserved phrase `答案是`. Their prompts,
hints, review notes, IDs, and linguistic conclusions are unchanged.

## Merged candidate validation

The three base files were merged in order and the two overlay objects replaced
their matching IDs.

- Base rows: 120
- Overlay rows / replacements: 2 / 2
- Effective rows / unique IDs: 120 / 120
- Validator: 0 errors, 0 warnings
- Exact schema keys: 120 / 120
- EP21 / EP22 counts: 56 / 64
- Exercise-type counts:
  - `grammar_meaning`: 11
  - `sentence_understanding`: 45
  - `vocab_reading`: 55
  - `vocab_meaning`: 9
- Ordered comma-MD5: `eedf6b899655ddd940abeaa95b17ad29`
- Normalized duplicate prompts: 0
- Normalized duplicate answers: 0
- Normalized duplicate hints: 0
- Hint contains the complete normalized answer: 0
- Long answer/hint common-substring warnings: 0
- Learner-facing metadata or template leakage: 0
- BOM or zero-width-character findings: 0
- Target-ID overlap with other local candidate JSON files: 0

QA commands:

```text
node regen/merge_letter_batches35_37_for_qa.mjs C:\tmp\letter_batches35_37_merged_for_qa.json
node regen/validate_candidates.mjs C:\tmp\letter_batches35_37_merged_for_qa.json
node regen/qa_letter_batches35_37_overlay.mjs
```

## Source and association checks

- Subtitle content matches: 120 / 120.
  - Matching normalization removed only subtitle ruby parentheses, whitespace, and
    outer curly quotation marks.
  - Exercises 216 and 217 correspond to subtitle lines 62 and 63, whose stored
    subtitle text includes outer `“…”`; the internal Japanese text is unchanged.
- All 45 `sentence_understanding` source strings match `learning_sentences`.
- All 11 `grammar_meaning` source strings match the sentence corpus and were
  checked against the surrounding subtitle discourse; grammar records were used
  where a corresponding record exists.
- All 64 vocabulary exercises extracted a target surface, and 64 / 64 matched a
  `learning_vocab_items` surface; these represent 51 distinct surfaces.
- EP22 line 32 is genuinely stored as the character-stylized
  `信頼できるですか？`. The generated reading exercise preserves that source
  wording and explicitly limits the task to the reading of `信頼`.
- Repeated vocabulary targets such as `騎士`, `白鯨`, `討伐`, `魔女教`, and
  `信じる` use different contextual or phonological tasks rather than copied
  teaching text.

## Read-only database preflight

Project: `qoatvdvbuleamyzsaldp`

| Check | Result |
|---|---:|
| Current ordered targets | 120 |
| Unique target IDs | 120 |
| Backup rows | 120 |
| Full-row undrifted targets | 120 |
| Still-single-letter answers | 120 |
| Manifest rows touching targets | 0 |
| Applied manifest rows touching targets | 0 |
| First ID | `re-zero-s01e21-exercise-184` |
| Last ID | `re-zero-s01e22-exercise-064` |
| Ordered comma-MD5 | `eedf6b899655ddd940abeaa95b17ad29` |

The candidate set is frozen for independent review. It has not been applied to
`public.learning_exercises`.
