# Brainstorm: Next Features To Add

## Goal

Review the current Japanese study project after the recently completed review loop, study plan lifecycle, word entry management, and template CRUD work. Identify the highest-value remaining product gaps, propose concrete next-feature options, and converge on a practical MVP priority for the next implementation task.

## What I already know

* The project already supports word set creation/import, word entry CRUD and filtering, study plan create/update/lifecycle, daily card review, template CRUD/preview, and export job creation/download.
* Frontend routes currently include only `word-sets`, `study-plans`, `cards`, `templates`, and `export-jobs`.
* `frontend/src/features/study-plans/StudyPlanPage.tsx` already exposes study plan status transitions (`activate`, `pause`, `archive`) and template selection, so the core planning workflow is usable.
* `frontend/src/features/cards/TodayCardsPage.tsx` already supports today-card query, rating submission, and review history, but it is still an operator-style page rather than a learner dashboard.
* `docs/recommended-next-features.md` currently leaves these themes as remaining priorities:
  * study dashboard and aggregate visibility
  * import enhancement workflow
  * export workflow enhancement
  * metrics and long-range planning views
* `frontend/src/features/export-jobs/ExportJobPage.tsx` still creates export jobs with only `planId`, `exportType`, and `targetDate`; there is no explicit template selection or preview on the export page.
* `docs/system-usage-guide.md` states current known gaps:
  * no separate default template center
  * no explicit template selection in export creation UI
  * no dashboard / aggregate progress views

## Assumptions (temporary)

* The user wants to keep pushing the project toward a usable learning product, not just internal framework completeness.
* The next feature should preferably build on already-shipped workflows rather than open a completely new domain.
* A good next task should improve either:
  * daily usage visibility
  * import/export confidence
  * decision support for study progress

## Open Questions

* None. The next implementation direction has been confirmed.

## Requirements (evolving)

* Review current implemented scope from repo code and docs.
* Identify concrete missing features rather than vague "could improve" ideas.
* Group candidate features by user value and implementation leverage.
* Recommend a next MVP priority with explicit trade-offs.
* Produce a backlog entry that can be turned into the next implementation task with minimal re-discovery.
* The next implementation task should focus on a study dashboard / aggregate visibility feature.
* The first dashboard version should be the "standard" version rather than the minimal overview.

## Acceptance Criteria (evolving)

* [ ] Current implemented feature surface is summarized from code/docs.
* [ ] Remaining feature gaps are grouped into clear candidate tracks.
* [ ] At least 2-3 realistic next-step options are proposed with trade-offs.
* [ ] One recommended next MVP direction is identified.

## Definition of Done (team quality bar)

* PRD reflects the current codebase state
* Recommendation is based on repo evidence, not guesswork
* Scope boundaries for the recommended next task are explicit
* Follow-up implementation can start from this task without re-discovery

## Out of Scope (explicit)

* Implementing the next feature in this brainstorm task
* Re-architecting the whole product roadmap
* Non-repo market research beyond what is needed to prioritize the next task

## Technical Notes

* Inspected:
  * `docs/recommended-next-features.md`
  * `docs/system-usage-guide.md`
  * `docs/api-specification.md`
  * `frontend/src/app/router.tsx`
  * `frontend/src/features/export-jobs/ExportJobPage.tsx`
  * `frontend/src/features/study-plans/StudyPlanPage.tsx`
  * `frontend/src/features/cards/TodayCardsPage.tsx`
* Current route/module inventory suggests no dashboard/statistics surface exists yet.
* Export flow is functional but thin: create + list + download, with no template choice or preview in the UI.
* Current card and plan pages prove the core study loop is present, so the biggest remaining product gap is visibility and decision support rather than raw CRUD coverage.

## Candidate Tracks

### Option A: Study dashboard and aggregate visibility (Recommended)

* Why now:
  * the project already has plan lifecycle, daily cards, and review history
  * users still need to jump across multiple pages and manually input plan/date to understand progress
* Likely scope:
  * dashboard page / route
  * active plan summary cards
  * daily / weekly new-vs-review counts
  * plan progress and recent review activity snapshot
* Trade-off:
  * requires at least one new aggregate query instead of only reusing existing CRUD endpoints
  * slightly larger cross-layer scope, but it closes the most obvious usability gap

### Option B: Import enhancement workflow

* Why now:
  * import quality directly affects all downstream learning data
  * `.apkg` / CSV input errors are costly to discover late
* Likely scope:
  * pre-import validation preview
  * duplicate/conflict summary before apply
  * clearer field-mapping and row-level diagnostics
* Trade-off:
  * high operational value but less visible during everyday study once data is already loaded
  * more parsing/validation complexity

### Option C: Export workflow enhancement

* Why now:
  * current export page is clearly thinner than template and plan flows
  * there is an obvious missing UI for template selection and preview
* Likely scope:
  * explicit template selection in export create form
  * format-aware preview / validation hints
  * clearer failed export diagnostics
* Trade-off:
  * probably the lowest implementation risk
  * user value is narrower than dashboard because it improves an occasional workflow rather than daily usage

## Recommendation

Recommend taking **Option A: study dashboard and aggregate visibility** as the next implementation task.

Reasoning:

* core study loop features are already in place, so the next biggest gap is "what should I do now" visibility
* dashboard work strengthens daily retention and product usability instead of only improving admin-style flows
* export and import enhancements remain important, but they are better positioned as the next follow-up tasks after visibility is in place

## Decision (ADR-lite)

**Context**: the repo already supports study plan lifecycle, daily cards, review submission, template CRUD, and exports, but still lacks a unified visibility surface for daily learning decisions.

**Decision**: prioritize a study dashboard / aggregate visibility feature as the next task, and target the "standard" first version.

**Consequences**:

* next task should center on learner-facing visibility instead of more admin-style CRUD work
* first version should include not only overview cards, but also recent trend / comparison modules
* implementation will likely need at least one aggregate query and one new frontend route/page
* import/export enhancements remain in backlog but are no longer the immediate next priority

## Standard Dashboard Scope Draft

The currently selected first-version scope is:

* today's study overview
* active plan summary cards
* recent 7-day new/review trend
* cross-plan progress comparison

Intentionally excluded for this version:

* long-term streak / monthly analytics
* detailed stage distribution analysis
* import/export workflow improvements

## Final Confirmation

Confirmed implementation direction:

* create a standalone `/dashboard`
* change `/` default entry to `/dashboard`
* place dashboard as the first left-nav item
* keep `word-sets` as a data-management page instead of the default home
* implement the standard dashboard scope:
  * today's study overview
  * active plan summary
  * recent 7-day new/review trend
  * cross-plan comparison

Follow-up task created:

* `.trellis/tasks/04-20-implement-study-dashboard-as-home`
