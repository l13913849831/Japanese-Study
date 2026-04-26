# Current Feature Workflows

> Migrated from retired OpenSpec changes and synced to the frontend routes and API clients that are currently implemented.

---

## Source of Truth

Use this file when the change touches a learner-facing page, route, query key, or mutation flow.

This file absorbs the retired OpenSpec route and interaction notes for bootstrap, core workflows, and `.apkg` import.

When this file, `docs/system-usage-guide.md`, and the page code disagree, prefer the page code first, then update the docs.

---

## Scenario: App shell and route entry

### Scope

- App shell layout
- route registration
- default landing path
- left navigation ownership

### Current routes

- `/` redirects to `/dashboard`
- `/dashboard`
- `/word-sets`
- `/study-plans`
- `/cards`
- `/notes/dashboard`
- `/notes`
- `/notes/review`
- `/templates`
- `/export-jobs`

### Current contract

- `/dashboard` is the default learner entry.
- `/dashboard` acts as a unified workbench, not a word-study-only summary page.
- Navigation is route-driven from `AppShellLayout`.
- Each page owns its own query and mutation wiring; shared shell code stays thin.

### Tests Required

- router smoke test for `/` redirect
- shell navigation rendering test
- route-level page mount smoke tests

### Wrong vs Correct

#### Wrong

- let the shell page absorb feature logic
- keep a stale home route that points to `/word-sets`

#### Correct

- keep the shell focused on layout and navigation only
- treat `/dashboard` as the default learner entry

---

## Scenario: Word-set preparation and import preview

### Scope

- word-set list/create
- word-entry list/create/update/delete
- import preview modal and apply action
- CSV / APKG upload entry

### Query and mutation anchors

- `["wordSets"]`
- `["wordEntries", wordSetId, filters]`
- preview uses mutation, not query caching
- import apply uses mutation, then invalidates word-set and word-entry data

### Current contract

- The user must select a word set before importing.
- Upload opens preview first; it does not import immediately.
- Preview modal surfaces:
  - source type
  - ready / duplicate / error counts
  - field-mapping diagnostics
  - row-level readiness
- Only `READY` rows are imported after confirmation.
- The page keeps showing the last import result summary after apply.

### Tests Required

- word-set selection guard before import
- preview modal rendering for mixed row states
- import confirmation invalidation coverage
- word-entry table filter and CRUD coverage

### Wrong vs Correct

#### Wrong

- call import directly from upload without preview
- duplicate result rendering logic between CSV and APKG

#### Correct

- keep one preview-first flow for both file types
- reuse the same modal and result-summary path

---

## Scenario: Study plans, today cards, review, and dashboard

### Scope

- study-plan create/edit/list
- lifecycle actions: activate, pause, archive
- today-card query
- review submission and review-history query
- dashboard aggregate view

### Query and mutation anchors

- `["studyPlans"]`
- `["studyPlan", id]`
- `["todayCards", planId, date]`
- `["dashboard", date]`

### Current contract

- Study-plan page refreshes shared plan data after create, update, or lifecycle action.
- Template selectors reuse the same template list query keys as the template page.
- `/cards` defaults to the remembered active plan when one exists; if that plan is gone, it falls back to the first available active plan, then the first available plan.
- `/cards` opens with the current card as the visual primary area; plan/date controls and queue details stay available, but as secondary session controls.
- The current card resolves from the first pending item by default, but the queue still allows manual jumps without leaving the page.
- Review submission invalidates the current today-card query and current history, then advances the session to the next pending card when one exists.
- Word-study aggregates still come from the study dashboard API, but the learner-facing `/dashboard` page now combines them with note-review aggregates into one workbench.
- The workbench keeps direct jump actions into `/cards` with the relevant plan context and frames them as continuing today's session instead of opening a setup page.
- Word-study sections inside the workbench still show:
  - overview stats
  - active plan summary cards
  - recent seven-day trend

### Tests Required

- study-plan form validation and lifecycle-action coverage
- today-card fetch, default-plan fallback, current-card resolution, and review-submit invalidation coverage
- workbench empty/loading/error state coverage
- dashboard navigation jump coverage

### Wrong vs Correct

#### Wrong

- hardcode template choices in study-plan forms
- compute dashboard numbers separately from backend response

#### Correct

- reuse shared template queries
- render the workbench directly from the aggregate API contracts and preserve direct actions into the underlying study flows

---

## Scenario: Word-review FSRS migration

### Scope

- `/study-plans` wording around compatibility fields
- `/cards` queue source semantics
- dashboard word-study summaries after FSRS migration

### Query and mutation anchors

- `["studyPlans"]`
- `["todayCards", planId, date]`
- `["cardReviews", cardId]`
- `["dashboard", date]`

### Current contract

- `/study-plans` still sends `reviewOffsets`, but the UI must treat it as compatibility data rather than the active schedule model.
- `/cards` now reads one pending runtime row per word from the backend. The frontend still owns same-day requeue and weak-round behavior.
- After a card review succeeds, the backend appends the next pending FSRS row. The frontend must not assume the old pre-generated offset rows still exist.
- Dashboard word-study counts now reflect pending rows selected by `dueAt`/day-end semantics instead of exact pre-generated due dates.

### Tests Required

- study-plan page wording and payload-shape regression coverage
- `/cards` session still works when backend creates next pending rows dynamically
- dashboard rendering coverage after FSRS-driven due counts change

### Wrong vs Correct

#### Wrong

- describe `reviewOffsets` as the live review schedule after FSRS migration
- assume the backend still pre-generates every future review row

#### Correct

- keep compatibility fields visible but clearly demoted
- treat `/cards` as a runtime queue backed by dynamic FSRS rows

---

## Scenario: Unified learner workbench

### Scope

- `/dashboard` route semantics
- combined today summary across word study and note review
- quick-start entry points into word and note flows

### Query and mutation anchors

- `["dashboard", date]`
- `["noteDashboard", date]`

### Current contract

- `/dashboard` is the main learner-facing workbench.
- The workbench combines existing study and note dashboard aggregates on the frontend instead of requiring a new backend endpoint.
- The workbench must surface:
  - combined today workload
  - word-study quick start
  - note-review quick start
  - deep links into plans, word sets, notes, and note dashboard
- Word review entry should reuse `currentPlanId` when the user jumps from the workbench into `/cards`.
- Quick-start labels should make it clear that the user is continuing today's review session.

### Tests Required

- combined workbench summary rendering coverage
- partial failure handling when one aggregate query fails
- quick-start navigation coverage for word and note entry points

### Wrong vs Correct

#### Wrong

- keep `/dashboard` as a word-only page after notes became a first-class study line
- duplicate backend aggregation just to assemble the first version of the workbench

#### Correct

- treat `/dashboard` as the unified daily entry
- reuse existing aggregate APIs first and only add backend composition later if needed

---

## Scenario: Weak-item recovery loop and weak-items page

### Scope

- `/cards` session queue behavior
- `/notes/review` session queue behavior
- `/weak-items` route and tabs
- workbench weak-item entry

### Query and mutation anchors

- `["todayCards", planId, date]`
- `["cardReviews", cardId]`
- `["todayNoteReviews", date]`
- `["noteReviewLogs", noteId]`
- `["weakItemSummary"]`
- `["weakWords"]`
- `["weakNotes"]`

### Current contract

- `/cards` keeps session-only queue state on the frontend instead of asking the backend to persist today's temporary queue.
- Word review rules:
  - first `AGAIN` appends one `REQUEUE` row to today's main queue
  - second `AGAIN` appends one `WEAK` row to the weak round
  - after the main queue ends, the page prompts whether to enter the weak round
- `/notes/review` keeps session-only recovery state on the frontend.
- Note review rules:
  - first `AGAIN` appends one `RECOVERY` row to today's main queue
  - second `AGAIN` appends one `WEAK` row to the weak round
  - after the main queue ends, the page prompts whether to enter the weak round
- Review submission sends `sessionAgainCount` so the backend can decide whether to mark the item `WEAK`.
- Both `/cards` and `/notes/review` keep their setup controls and compact queue visible as supporting tools, but the current item remains the visual primary area.
- `/weak-items` shows:
  - summary cards
  - `易错词` tab
  - `易错知识点` tab
  - dismiss actions for both tabs
- `/dashboard` surfaces weak-item counts and links into `/weak-items`.

### Tests Required

- word-review queue regression coverage for first `AGAIN`, second `AGAIN`, and weak-round prompt
- note-review queue regression coverage for recovery queue, weak round, and prompt
- weak-items summary/list/dismiss query invalidation coverage
- workbench weak-item quick-entry rendering coverage

### Wrong vs Correct

#### Wrong

- rely on refetching `todayCards` or `todayNoteReviews` to rebuild today's temporary queue
- lose the distinction between main queue, recovery queue, and weak round
- hide weak-item entry only inside `/weak-items` without surfacing it from the workbench

#### Correct

- keep temporary recovery queue state on the frontend session
- send only `sessionAgainCount` and final rating to the backend for long-term weak-state changes
- surface weak-item counts from the workbench and route users into `/weak-items`

---

## Scenario: Template editing, preview, and export delivery

### Scope

- template create/update flows
- draft preview flows
- export create/list/download flows

### Query and mutation anchors

- `["ankiTemplates"]`
- `["markdownTemplates"]`
- `["exportJobs"]`

### Current contract

- Template page is not read-only.
- Anki and Markdown templates both support create, update, and draft preview.
- Preview requests are built from current form state, not from persisted IDs only.
- After template save, shared template queries are refreshed so study-plan selectors can see the latest templates immediately.
- Export page creates jobs from selected plan, export type, and target date, then lists and downloads generated files.

### Tests Required

- template save and preview request-shape coverage
- shared query refresh coverage after template save
- export create/list/download interaction coverage

### Wrong vs Correct

#### Wrong

- keep a separate template cache path for study plans
- make export creation bypass the list refresh path

#### Correct

- share query keys across template consumers
- keep export creation and list refresh in the same page workflow

---

## Scenario: Notes CRUD, Markdown preview import, review queue, and note dashboard

### Scope

- note list/create/update/delete
- Markdown upload preview and import confirmation
- note review queue, reveal-answer flow, and review history
- note dashboard overview and entry links

### Query and mutation anchors

- `["notes", page, pageSize, keyword, tag, masteryStatus]`
- `["noteDashboard", date]`
- `["todayNoteReviews", date]`
- `["noteReviewLogs", noteId]`
- import preview uses mutation state, not query caching

### Current contract

- Notes are independent from word-set and study-plan pages.
- `/notes/dashboard` is the aggregate entry for note review.
- `/notes` owns both manual CRUD and Markdown preview-first import.
- Upload always opens preview first; preview rows can be edited or removed before apply.
- Common tags prefill each preview item and remain editable per row on the page side.
- `/notes/review` uses the same focused session shape as `/cards`: one current item, one compact queue, one history area.
- `/notes/review` defaults straight into today's queue for the selected date instead of making the user start from a setup-first layout.
- `/notes/review` starts with title recall, then reveals content on demand, then submits one of the four ratings.
- Queue clicks can switch the current note, but scoring should keep the main path moving forward instead of returning to a table-driven workflow.
- Review submit invalidates note list, dashboard, queue, and selected history data.

### Tests Required

- notes table filter and CRUD interaction coverage
- Markdown preview draft editing and import confirmation coverage
- note-review queue fetch, reveal flow, current-note switching, and submit invalidation coverage
- dashboard empty/loading/error and navigation-entry coverage

### Wrong vs Correct

#### Wrong

- bypass preview and import immediately after upload
- mix note review data into existing `cards` page state
- let `/notes` menu highlighting override `/notes/review`

#### Correct

- keep one preview-first import path
- keep notes routes and query keys isolated from word review
- use longest-prefix route matching so review/detail subroutes highlight correctly

---

## Migration Note

OpenSpec is retired.

Use Trellis specs plus the current page code as the execution contract. If an old note conflicts with the route tree or API client shape, update Trellis instead of reviving the retired workflow.
