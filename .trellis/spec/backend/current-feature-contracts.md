# Current Feature Contracts

> Migrated from retired OpenSpec changes and synced to the backend that is currently implemented.

---

## Source of Truth

Use this file when the change touches any implemented learner-facing backend flow.

This file absorbs the retired OpenSpec feature contracts for bootstrap, core workflows, and `.apkg` import.

When Trellis, `docs/api-specification.md`, and code disagree, prefer current code first, then update both docs.

---

## Scenario: Word-set data preparation and import

### 1. Scope / Trigger

- Trigger: change touches `wordset` controllers, import parsing, word-entry CRUD, or import preview/apply behavior.
- Packages: `com.jp.vocab.wordset`, shared API envelope, frontend `/word-sets`.

### 2. Signatures

- `POST /api/word-sets`
- `GET /api/word-sets`
- `GET /api/word-sets/{wordSetId}/words?page=&pageSize=&keyword=&level=&tag=`
- `POST /api/word-sets/{wordSetId}/words`
- `PUT /api/words/{wordId}`
- `DELETE /api/words/{wordId}`
- `POST /api/word-sets/{wordSetId}/import/preview` with `multipart/form-data`
- `POST /api/word-sets/{wordSetId}/import` with `multipart/form-data`

### 3. Contracts

- Import supports `CSV` and `APKG`.
- Preview returns:
  - `sourceType`
  - `totalRows`
  - `readyCount`
  - `duplicateCount`
  - `errorCount`
  - `fieldMappings[]`
  - `previewRows[]`
- Preview row status is one of:
  - `READY`
  - `DUPLICATE`
  - `ERROR`
- Preview does not persist data.
- Apply persists only rows that were effectively valid for import and returns:
  - `importedCount`
  - `skippedCount`
  - `errors[]`
- Duplicate detection is scoped to the target word set and keyed by `expression + reading`.
- `.apkg` import reuses the same save pipeline as CSV after field mapping and cleanup.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| unsupported file extension | reject with standard error envelope |
| missing required CSV/APKG fields | preview still returns diagnostics and row-level `ERROR` |
| duplicate row | preview marks `DUPLICATE`; apply increments `skippedCount` |
| unreadable `.apkg` package | reject with import error, do not partially persist |
| alias-based CSV match | surface it through `fieldMappings[].note` |

### 5. Good / Base / Bad Cases

- Good: valid CSV or `.apkg`, required fields mapped, preview shows `READY`, apply imports rows in source order.
- Base: preview includes a mix of `READY`, `DUPLICATE`, and `ERROR`; apply still imports only valid rows.
- Bad: package cannot expose readable note data or required fields never map to `expression` and `meaning`.

### 6. Tests Required

- controller test for preview/apply multipart endpoints
- CSV preview and apply regression coverage
- `.apkg` parsing and field-mapping coverage
- duplicate handling and source-order preservation coverage
- word-entry list filter and CRUD coverage

### 7. Wrong vs Correct

#### Wrong

- create a separate `.apkg`-only response contract
- persist rows during preview
- duplicate CSV and `.apkg` validation logic in different save paths

#### Correct

- keep one preview/apply result model for both file types
- treat preview as pure analysis
- convert both sources into the same analyzed-row/save pipeline

---

## Scenario: Study-plan runtime, cards, review, and dashboard

### 1. Scope / Trigger

- Trigger: change touches `studyplan`, `card`, `dashboard`, or the state transition between plan, generated cards, and review logs.
- Packages: `com.jp.vocab.studyplan`, `com.jp.vocab.card`, `com.jp.vocab.dashboard`.

### 2. Signatures

- `GET /api/study-plans`
- `GET /api/study-plans/{id}`
- `POST /api/study-plans`
- `PUT /api/study-plans/{id}`
- `POST /api/study-plans/{id}/activate`
- `POST /api/study-plans/{id}/pause`
- `POST /api/study-plans/{id}/archive`
- `GET /api/study-plans/{planId}/cards/today?date=YYYY-MM-DD`
- `GET /api/study-plans/{planId}/cards/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD`
- `POST /api/cards/{cardId}/review`
- `GET /api/cards/{cardId}/reviews`
- `GET /api/dashboard?date=YYYY-MM-DD`

### 3. Contracts

- Study plan status is one of `DRAFT`, `ACTIVE`, `PAUSED`, `ARCHIVED`.
- `reviewOffsets` must be non-empty, sorted ascending, and start with `0`.
- Plan create/update returns the full persisted plan object.
- Plan create and rebuild paths work against pre-generated `card_instance` records.
- Today cards return real card instances plus word-entry fields needed by the UI.
- Review submission appends a review log and returns:
  - `reviewId`
  - `cardId`
  - `rating`
  - `cardStatus`
  - `reviewedAt`
- Dashboard is aggregate-only:
  - `overview`
  - `activePlans[]`
  - `recentTrend[]`
- Dashboard adds no new persistence model; it reuses `study_plan`, `card_instance`, and `review_log`.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| invalid `reviewOffsets` | reject with field-level validation error |
| missing word set or template reference | reject before plan persistence |
| lifecycle action not allowed for current status | reject with business error |
| review request for unknown card | reject with standard error envelope |
| dashboard date omitted or invalid | validate at controller boundary before aggregation |

### 5. Good / Base / Bad Cases

- Good: create a valid plan, generate cards, activate it, query today cards, submit reviews, and see dashboard aggregates move.
- Base: pause or archive a plan without losing its persisted history.
- Bad: mutate key plan inputs without rebuilding dependent cards, or compute today cards ad hoc outside `card_instance`.

### 6. Tests Required

- plan create/update/lifecycle controller coverage
- card generation service coverage for offsets, sequence, and due dates
- today/calendar query coverage
- review submit and review-history coverage
- dashboard aggregation coverage for mixed active-plan states

### 7. Wrong vs Correct

#### Wrong

- recalculate cards only at query time
- let review-offset rules drift between frontend and backend
- add dashboard-only tables for data that already exists

#### Correct

- keep generated cards as the runtime source for today/calendar flows
- validate lifecycle and offset rules at the backend boundary
- treat dashboard as an aggregate read model over existing tables

---

## Scenario: Note knowledge capture, Markdown import, note review, and dashboard

### 1. Scope / Trigger

- Trigger: change touches `note` controllers, Markdown parsing/import, note review scheduling, or note dashboard aggregation.
- Packages: `com.jp.vocab.note`, shared API envelope, frontend `/notes*`.

### 2. Signatures

- `GET /api/notes?page=&pageSize=&keyword=&tag=&masteryStatus=`
- `POST /api/notes`
- `PUT /api/notes/{noteId}`
- `DELETE /api/notes/{noteId}`
- `POST /api/notes/import/preview` with `multipart/form-data`
- `POST /api/notes/import`
- `GET /api/notes/reviews/today?date=YYYY-MM-DD`
- `POST /api/notes/{noteId}/reviews`
- `GET /api/notes/{noteId}/reviews`
- `GET /api/notes/dashboard?date=YYYY-MM-DD`
- DB tables:
  - `note`
  - `note_review_log`

### 3. Contracts

- Note is an independent learner-facing entity and stores:
  - `title`
  - `content`
  - `tags[]`
  - `reviewCount`
  - `masteryStatus`
  - `dueAt`
  - `lastReviewedAt`
  - `fsrsCardJson`
- Markdown import preview accepts:
  - `file`
  - `splitMode` in `H1 | H1_H2 | ALL`
  - `commonTagsText`
- Preview returns:
  - `splitMode`
  - `totalItems`
  - `readyCount`
  - `errorCount`
  - `previewItems[]`
- Untitled Markdown content is preserved as editable preview item titled `未命名知识点`.
- Empty parent headings created only as structure must not surface as preview rows.
- Review ratings are fixed to:
  - `AGAIN`
  - `HARD`
  - `GOOD`
  - `EASY`
- Product wording maps to FSRS as:
  - `不会 -> AGAIN`
  - `吃力 -> HARD`
  - `还行 -> GOOD`
  - `熟悉 -> EASY`
- Review submission appends one `note_review_log` row and updates the `note` scheduling fields from FSRS.
- Dashboard is aggregate-only and returns:
  - `overview`
  - `masteryDistribution[]`
  - `recentTrend[]`
  - `recentNotes[]`

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| empty or non-`.md` upload | reject with standard import error envelope |
| blank note title/content on create or import | reject with validation error |
| note not found | reject with standard `NOT_FOUND` envelope |
| invalid review rating | reject with validation error |
| blank section generated only from parent heading | skip it from preview instead of emitting an error row |

### 5. Good / Base / Bad Cases

- Good: create a note, import Markdown with preview edits, submit reviews, and see queue/dashboard values move.
- Base: preview contains both untitled content and titled sections; user edits tags and imports only the kept rows.
- Bad: persist Markdown preview rows before confirmation, or couple notes to the legacy `study_plan/card_instance/review_log` tables.

### 6. Tests Required

- Markdown parser coverage for untitled content preservation and breadcrumb titles

---

## Scenario: Weak-item lifecycle and same-day recovery

### 1. Scope / Trigger

- Trigger: change touches weak-item state, card/note review responses, weak-item listing APIs, or same-day recovery semantics.
- Packages: `com.jp.vocab.card`, `com.jp.vocab.note`, `com.jp.vocab.weakitem`, frontend `/cards`, `/notes/review`, `/weak-items`.

### 2. Signatures

- DB migration:
  - `V8__add_weak_state.sql`
- `POST /api/cards/{cardId}/review`
- `POST /api/notes/{noteId}/reviews`
- `GET /api/weak-items/summary`
- `GET /api/weak-items/words?page=&pageSize=`
- `GET /api/weak-items/notes?page=&pageSize=`
- `POST /api/weak-items/words/{cardId}/dismiss`
- `POST /api/weak-items/notes/{noteId}/dismiss`

### 3. Contracts

- `card_instance` adds:
  - `weak_flag`
  - `weak_marked_at`
  - `weak_review_count`
  - `last_review_rating`
- `note` adds:
  - `weak_flag`
  - `weak_marked_at`
  - `last_review_rating`
- Card review request accepts optional `sessionAgainCount`.
- Note review request accepts optional `sessionAgainCount`.
- Card review response returns:
  - `reviewId`
  - `cardId`
  - `rating`
  - `cardStatus`
  - `reviewedAt`
  - `weak`
  - `weakMarkedAt`
  - `todayAction`
- Note review response returns:
  - `reviewId`
  - `noteId`
  - `rating`
  - `masteryStatus`
  - `reviewedAt`
  - `dueAt`
  - `weak`
  - `weakMarkedAt`
  - `todayAction`
- `todayAction` values:
  - card: `DONE | REQUEUE_TODAY | MOVE_TO_WEAK_ROUND`
  - note: `DONE | MOVE_TO_RECOVERY_QUEUE | MOVE_TO_WEAK_ROUND`
- `GET /api/weak-items/summary` returns:
  - `weakWordCount`
  - `weakNoteCount`
- Weak-item list endpoints are paged and sorted by `weak_marked_at desc, id desc`.
- Dismiss endpoints clear weak state only; they do not mutate review logs.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| `sessionAgainCount < 0` | reject with validation error |
| second-or-later `AGAIN` in same session | mark item weak and return `MOVE_TO_WEAK_ROUND` |
| first `AGAIN` for card | keep long-term schedule path, return `REQUEUE_TODAY` |
| first `AGAIN` for note | keep long-term schedule path, return `MOVE_TO_RECOVERY_QUEUE` |
| later `GOOD` or `EASY` on weak item | clear weak state automatically |
| dismiss unknown card/note | reject with standard `NOT_FOUND` envelope |

### 5. Good / Base / Bad Cases

- Good: card or note hits `AGAIN` twice in the same session, enters weak list, appears in `/weak-items`, then exits on later `GOOD`.
- Base: item enters weak state and the learner manually dismisses it from `/weak-items`.
- Bad: persist today's temporary recovery queue in the database or require a dedicated temp table just to support same-day requeue.

### 6. Tests Required

- card review service coverage for `sessionAgainCount`, weak marking, and `GOOD/EASY` weak clearing
- note review service coverage for `sessionAgainCount`, weak marking, and `GOOD/EASY` weak clearing
- weak-item controller/service coverage for summary, paged list, and dismiss behavior
- migration verification for new weak-state columns and constraints

### 7. Wrong vs Correct

#### Wrong

- treat same-day recovery queue as persisted backend state
- mark items weak on first `AGAIN` without session context
- overload review logs to act as the weak-item read model

#### Correct

- keep same-day recovery as frontend session state and persist only long-term weak state
- use `sessionAgainCount` to distinguish first and second `AGAIN`
- expose weak-item summary/list/dismiss through dedicated read endpoints
- FSRS scheduler coverage for initial state and mastery bucketing
- CRUD, import preview/apply, review, and dashboard controller/service coverage
- migration assertions for note status/rating constraints and note-review foreign key behavior

### 7. Wrong vs Correct

#### Wrong

- store imported Markdown as a required parent document entity
- surface empty structural headings as editable note rows
- reuse the word-study runtime tables for note review

#### Correct

- treat imported output as first-class notes
- keep preview focused on real editable knowledge points
- keep note scheduling and review logs in dedicated note tables

---

## Scenario: Template preview and export delivery

### 1. Scope / Trigger

- Trigger: change touches template CRUD/preview, export generation/download, or shared rendering context.
- Packages: `com.jp.vocab.template`, `com.jp.vocab.exportjob`.

### 2. Signatures

- `GET /api/templates/anki`
- `POST /api/templates/anki`
- `PUT /api/templates/anki/{id}`
- `POST /api/templates/anki/preview`
- `GET /api/templates/md`
- `POST /api/templates/md`
- `PUT /api/templates/md/{id}`
- `POST /api/templates/md/preview`
- `GET /api/export-jobs`
- `POST /api/export-jobs`
- `GET /api/export-jobs/{id}/download`

### 3. Contracts

- Preview routes are request-body driven and do not require template IDs.
- Anki preview request carries template strings plus one `sample` card context.
- Anki preview response returns:
  - `frontRendered`
  - `backRendered`
  - `cssRendered`
- Markdown preview request carries:
  - `templateContent`
  - `date`
  - `planName`
  - `newCards[]`
  - `reviewCards[]`
- Markdown preview response returns `renderedContent`.
- Export create supports:
  - `ANKI_CSV`
  - `ANKI_TSV`
  - `MARKDOWN`
- Download serves the generated file for a completed export job.

### 4. Validation & Error Matrix

| Trigger | Expected behavior |
|---------|-------------------|
| unsupported template variable or syntax error | map to standardized preview error response |
| missing export target plan or date | reject before job creation |
| unsupported export type | reject with validation or business error |
| download before file generation is ready | reject with standard error envelope |

### 5. Good / Base / Bad Cases

- Good: save template, preview with sample data, create export job, list it, then download the generated file.
- Base: refresh template lists and reuse them immediately in study-plan selectors.
- Bad: couple preview to stored template IDs only, or bypass export-job persistence when generating files.

### 6. Tests Required

- template create/update/preview controller coverage
- preview rendering error-path coverage
- export create/list/download coverage
- file-name and content-type assertions for each export type

### 7. Wrong vs Correct

#### Wrong

- require database persistence before every preview
- invent a preview-only variable shape that diverges from export rendering
- stream ad hoc files without recording export-job state

#### Correct

- keep preview request-body driven
- share rendering expectations between preview and export
- keep export generation and download anchored to `export_job`

---

## Migration Note

The retired OpenSpec work is now treated as Trellis-owned knowledge.

The main remaining debt from those original task lists is test coverage and regression verification, not missing feature scope.
