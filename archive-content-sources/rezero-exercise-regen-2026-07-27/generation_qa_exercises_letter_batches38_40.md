# Letter-answer exercise generation QA: batches 38–40

## Scope and authorship boundary

- Frozen selection: `regen/selection_next_letter_batches38_40.json`.
- Selection size: 120 current single-letter-answer rows, all from Re:Zero episode 22.
- Batch 38: 40 rows, exercises 065–074 and 076–105.
- Batch 39: 40 rows, exercises 106–145.
- Batch 40: 40 rows, exercises 146–185.
- Ordered comma-MD5 of the 120 selected IDs:
  `da2a8fa9be8c6c8581d2c01febf97560`.
- Selection SHA256:
  `8AA52EEA58AB8E3C92DFFCAD47D8B8C0DCD1AD7D9CBF2153418100617E6B57F2`.
- The teaching fields were written manually, one ID at a time, after reading the
  current exercise, associated vocabulary/grammar/sentence records, current
  Japanese subtitle, and adjacent turns.
- No Python, JavaScript, PowerShell, or SQL template generated `prompt`,
  `answer`, `hint`, or `review_note`. Scripts and shell checks were used only for
  selection, validation, hashing, overlap checks, and read-only database queries.
- This is generation-side source and mechanical QA, not independent review.
  These candidates are frozen for a different Agent to review and are not
  approved for database application yet.
- This task performed no database write.

## Candidate files

| File | Rows | Source-type mix | SHA256 |
|---|---:|---|---|
| `regen/batch38_exercises_letter.json` | 40 | 40 `vocab_meaning` | `629D4F813F35D48AA382E3017B68CBE6390B20A312A6805913BB1F9BCEED3A83` |
| `regen/batch39_exercises_letter.json` | 40 | 5 `vocab_meaning`, 35 `grammar_meaning` | `87E7B822BEA3C359D918B8BD61C1BE1378AA3006BBD712DF6A8D039487E5C967` |
| `regen/batch40_exercises_letter.json` | 40 | 17 `grammar_meaning`, 23 `sentence_understanding` | `87A8F66C15A73856210AA33700FDAA851F74A00968C11FD3772721218B037D02` |
| `regen/generation_overlay_exercises_letter_batches38_40.json` | 0 | replacement set, currently empty | `37517E5F3DC66819F61F5A7BB8ACE1921282415F10551D2DEFA5C3EB0985B570` |

The overlay is defined as an ID-keyed replacement set, never an append set. All
generation-side corrections found before the freeze were written directly into
the relevant base row, so this generation overlay is an empty JSON array. The
effective candidate therefore remains exactly 120 rows. The SHA256 of an
in-memory compact canonical serialization of the merged 120 rows is
`66F6EC1E6D5A1D558224487000174005201EEA87BC095F3588C372BA31D91C98`.

## Merged mechanical validation

The three base files were validated together in selection order.

- Base rows: 120
- Effective rows: 120
- Unique IDs: 120
- Missing / extra IDs versus frozen selection: 0 / 0
- Exact selection order match: yes
- Validator: 0 errors, 0 warnings
- Exact schema keys (`id`, `prompt`, `answer`, `hint`, `review_note`): 120 / 120
- Remaining single-letter or `用于…` placeholder answers: 0
- Exercise-type counts:
  - `vocab_meaning`: 45
  - `grammar_meaning`: 52
  - `sentence_understanding`: 23
- Normalized duplicate prompt groups: 0
- Normalized duplicate answer groups: 0
- Normalized duplicate hint groups: 0
- Hint contains the complete normalized answer: 0
- Learner-facing internal ID, option-answer, or template metadata leakage: 0
- BOM, zero-width, or CJK compatibility-radical findings: 0
- Overlap with other local candidate JSON files, excluding the frozen selection:
  0

Validation command:

```text
node regen/validate_candidates.mjs regen/batch38_exercises_letter.json regen/batch39_exercises_letter.json regen/batch40_exercises_letter.json
```

## Source and linguistic checks

Every row was checked for morphology, syntax, semantics, pragmatics, speaker and
addressee, reference, negation, tense/aspect, voice, and cross-subtitle context.
The checks used the current `subtitle_lines` text rather than relying on stale
translated subtitle alignment.

- All 45 vocabulary rows were traced to their current episode-22 line and
  relevant `learning_vocab_items` / occurrence record.
- All 52 grammar rows were checked against the current source sentence and the
  corresponding `learning_grammar_points` record where one exists. A generic
  grammar label was not retained when the surface form or discourse contradicted
  it.
- All 23 sentence rows exactly correspond to `learning_sentences` and current
  subtitle text. Their surrounding turns were read through the end of each
  exchange or briefing unit.
- Repeated source words and repeated sentences received different tasks tied to
  their actual local function; teaching text was not copied between rows.

Material generation-side corrections include:

1. Exercise 070: the final `命` in `運命` is not an independent token meaning
   “life”; the row now teaches the real boundary `運命｜様`.
2. Exercise 074: the source contains the verb `臭う`, not the adjective/noun
   label `臭い`; the row now checks the attested form and the witch-scent
   reference.
3. Exercise 077: `命` is only an internal character of `命令`; the row now
   preserves the compound and also analyzes `くれりゃ`.
4. Exercises 102 and 110: quoted `指先` refers to Petelgeuse's subordinate
   units, not anatomical fingertips.
5. Exercise 117: `唾つけときゃ` is restored as
   `唾をつけておけば`; its operative form is a conditional contraction, not
   bare preparatory `ておく`.
6. Exercise 124: `かましてくれた` is hostile irony, while
   `つけさせなきゃならねえ` contains causative plus obligation; it is not a
   simple benefactive.
7. Exercise 127: `あくまで` is a lexicalized adverb meaning “strictly / only
   within that role”, not separable range particle `まで`.
8. Exercise 129: `ついてくればいい` uses the conditional of `来る`; it is
   not `てくれる`.
9. Exercise 146: the sentence contains enumerative/reason-giving `し` and no
   `んだ／のだ`.
10. Exercise 154: current subtitle line 97 crosses a speaker boundary:
    Julius completes `心から悪かった`, then Subaru begins `けど…`; the row
    does not invent a same-speaker retraction of the apology.
11. Exercise 162: `お待ちしておりました` is humble `お待ちする` plus
    continuous `ておる／ている`, not preparatory `ておく`.

Other rows explicitly retain uncertainty where the Japanese leaves it open. For
example, exercise 098 does not pretend that the predicate omitted after
`ワタシの見えざる手を…` is uniquely recoverable, although the surrounding
turn licenses “saw” and “dodged” as contextual inferences.

## Read-only database preflight

Project: `qoatvdvbuleamyzsaldp`

The database was queried after all three files were written. No mutation was
performed.

| Guard | Result |
|---|---:|
| Current targets | 120 |
| Unique target IDs | 120 |
| Backup rows | 120 |
| Full-row target/backup matches | 120 |
| Still-single-letter answers | 120 |
| Any manifest rows touching targets | 0 |
| Applied manifest rows touching targets | 0 |

The generation set is therefore still undrifted and non-overlapping at freeze
time. These figures are only a read-only generation preflight; the transactional
transport must repeat every guard after independent review and immediately
before any write.

## Independent-review handoff

- Required review scope: all 120 IDs, not a sample.
- A reviewer must be a different Agent from the generator.
- Any review correction must replace the matching base object by `id` in
  `regen/review_overlay_exercises_letter_batches38_40.json`; it must not append a
  second effective row.
- The reviewer should record complete ID coverage and decisions in
  `regen/cross_review_exercises_letter_batches38_40.md`.
- No candidate from this set may be written until that independent review has
  complete unique coverage and the post-review validator reports zero errors and
  zero warnings.
