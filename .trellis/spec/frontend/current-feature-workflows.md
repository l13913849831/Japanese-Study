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
- `/templates`
- `/export-jobs`

### Current contract

- The dashboard is the default entry point.
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
- Today-cards page is driven by selected plan and date.
- Review submission invalidates the current today-card query and refreshes the card state.
- Dashboard is aggregate-first:
  - overview stats
  - active plan summary cards
  - recent seven-day trend
- Dashboard rows can jump into `/cards` with the relevant plan context.

### Tests Required

- study-plan form validation and lifecycle-action coverage
- today-card fetch and review-submit invalidation coverage
- dashboard empty/loading/error state coverage
- dashboard navigation jump coverage

### Wrong vs Correct

#### Wrong

- hardcode template choices in study-plan forms
- compute dashboard numbers separately from backend response

#### Correct

- reuse shared template queries
- render dashboard directly from the aggregate API contract

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

## Migration Note

OpenSpec is retired.

Use Trellis specs plus the current page code as the execution contract. If an old note conflicts with the route tree or API client shape, update Trellis instead of reviving the retired workflow.
