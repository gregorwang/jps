# Letter-answer exercises batches 5–7 cross-review

## Scope and method

- Reviewed `batch5_exercises_letter.json`, `batch6_exercises_letter.json`, and `batch7_exercises_letter.json`: 120 regenerated exercises in total.
- Matched the candidate IDs against the original `learning_exercises` rows in Supabase and checked every exercise against its cited `learning_grammar_points`, `learning_sentences`, or `learning_vocab_items` source.
- Read the authoritative Japanese subtitles around every cited scene in EP16 and EP17. The shifted Chinese subtitle field was not used as evidence.
- Checked reference mapping, speaker identity, honorific and benefactive direction, contextual completeness, answer accuracy, and hint leakage.
- This review made no database writes.

## Verdict

| File | Rows | Passed unchanged | Corrected | Rejected |
|---|---:|---:|---:|---:|
| `batch5_exercises_letter.json` | 40 | 40 | 0 | 0 |
| `batch6_exercises_letter.json` | 40 | 38 | 2 | 0 |
| `batch7_exercises_letter.json` | 40 | 35 | 5 | 0 |
| **Total** | **120** | **113** | **7** | **0** |

All seven corrected records pass after revision:

1. `re-zero-s01e16-exercise-228`
   - Corrected the participant direction in the EP16 lines 211–214 explanation.
   - Subaru is the one begging for help and metaphorically wagging his tail; Emilia is the master he would abandon. The previous wording incorrectly made the master the requester.
2. `re-zero-s01e16-exercise-232`
   - Replaced the unsupported description “the sword-wielder” with the subtitle-grounded “the enraged Priscilla.”
3. `re-zero-s01e17-exercise-026`
   - Corrected `でしょうに` from an independent rhetorical question to a contrary-to-expectation complaint that completes the preceding unfinished question.
   - Reworded the hint so it guides the learner without repeating a long answer substring.
4. `re-zero-s01e17-exercise-040`
   - Replaced the stronger, unsupported claim “fog controlled by the White Whale” with the evidence-bounded “fog area where the White Whale is.”
5. `re-zero-s01e17-exercise-008`
   - Corrected the speaker of `あんな巨体で空を泳ぐ` from Rem to Otto.
6. `re-zero-s01e17-exercise-009`
   - Corrected the speaker of `白鯨です` from Rem to Otto.
7. `re-zero-s01e17-exercise-010`
   - Corrected the contextual explanation so Otto, rather than Rem, is the person who identifies the White Whale.

## Source and coverage checks

- The three files contain exactly the 120 original one-letter-answer records in the reviewed ranges:
  - EP16: 77 rows, `re-zero-s01e16-exercise-162` through `re-zero-s01e16-exercise-240`.
  - EP17: 43 rows, `re-zero-s01e17-exercise-001` through `re-zero-s01e17-exercise-043`.
- Ordered candidate-ID MD5: `70e430b821a2c74235ad62ef4d444539`.
- Ordered database-ID MD5 for the same one-letter-answer set: `70e430b821a2c74235ad62ef4d444539`.
- EP16 grammar references `grammar-047` through `grammar-070`, EP16 sentence references, and all cited EP17 vocabulary IDs exist and match the Japanese examples used by the exercises.
- Honorific and benefactive directions were checked explicitly, including `しております`, `申し上げます`, `ご不在`, `おっしゃっていました`, `〜てくれ`, `〜てやる`, and `〜てやった`.

## Final QA

- Validator: 120 rows, 120 unique IDs, 0 errors, 0 warnings.
- Exact five-field schema (`id`, `prompt`, `answer`, `hint`, `review_note`): 120/120.
- Blank required fields: 0.
- Duplicate normalized prompts: 0.
- Duplicate normalized answers: 0.
- Remaining one-letter answers: 0.
- Template-answer markers: 0.
- Normalized hint/answer shared spans of 8 or more characters: 0.
- Rejected rows: 0.
