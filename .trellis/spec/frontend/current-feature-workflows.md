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
- `/cards` starts from plan/date selection, then stays in a focused review-session layout instead of a table-first management flow.
- The current card resolves from the first pending item by default, but the queue still allows manual jumps without leaving the page.
- Review submission invalidates the current today-card query and current history, then advances the session to the next pending card when one exists.
- Word-study aggregates still come from the study dashboard API, but the learner-facing `/dashboard` page now combines them with note-review aggregates into one workbench.
- The workbench keeps direct jump actions into `/cards` with the relevant plan context.
- Word-study sections inside the workbench still show:
  - overview stats
  - active plan summary cards
  - recent seven-day trend

### Tests Required

- study-plan form validation and lifecycle-action coverage
- today-card fetch, current-card resolution, and review-submit invalidation coverage
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
