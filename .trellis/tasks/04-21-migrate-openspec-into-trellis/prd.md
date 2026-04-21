# Migrate OpenSpec Knowledge Into Trellis

## Goal

Move completed OpenSpec feature knowledge into `.trellis/spec/` so Trellis becomes the active project workflow and code-spec source of truth.

## Requirements

- Capture implemented OpenSpec capabilities in Trellis backend/frontend/guides docs.
- Sync the migrated content to current code behavior instead of copying proposal-only wording.
- Record that the main remaining gap from legacy OpenSpec work is test coverage, not missing feature scope.
- Update repository-facing docs so Trellis is the active workflow reference.

## Acceptance Criteria

- [ ] `.trellis/spec/backend/` contains migrated backend feature contracts for implemented phase-one flows.
- [ ] `.trellis/spec/frontend/` contains migrated frontend route and interaction workflows for implemented phase-one flows.
- [ ] `.trellis/spec/guides/` contains a guide that maps legacy OpenSpec sources to current Trellis source-of-truth docs.
- [ ] Backend, frontend, and guide indexes link the new docs.
- [ ] `README.md` and docs that describe the active workflow point to Trellis instead of OpenSpec.

## Technical Notes

- Source material comes from `openspec/changes/bootstrap-phase-one-framework/`, `openspec/changes/implement-phase-one-core-workflows/`, and `openspec/changes/support-apkg-import/`.
- Current controllers, frontend routes, and API clients override stale OpenSpec details where behavior has already moved forward.
- Do not delete `openspec/` yet; treat it as legacy reference until the migration is validated.
