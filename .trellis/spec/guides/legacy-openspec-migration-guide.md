# Retired OpenSpec Guide

> **Purpose**: Help future work use Trellis as the only active source of truth after OpenSpec retirement.

---

## What Changed

OpenSpec feature work was moved into Trellis docs. Active implementation guidance now lives under `.trellis/spec/`.

Current Trellis targets:

- `backend/current-feature-contracts.md`
- `frontend/current-feature-workflows.md`

---

## When To Use This Guide

Read this guide when:

- you find an old OpenSpec note in chat, commit history, or copied text and are not sure whether it is still authoritative
- you start work on an implemented phase-one feature
- you need to decide whether a missing item is a feature gap or only test debt

---

## Decision Rule

Use this order:

1. current code
2. current Trellis spec
3. current product and API docs
4. historical notes outside the active Trellis docs

OpenSpec is retired. Trellis is the only active execution workflow.

---

## Feature Mapping

| Legacy capability | Current Trellis home |
|-------------------|----------------------|
| bootstrap platform + app shell | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| word-entry import and browse | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| study-plan lifecycle | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| card pre-generation and runtime | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| template preview | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| export delivery | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |
| `.apkg` import | `backend/current-feature-contracts.md`, `frontend/current-feature-workflows.md` |

---

## What Still Counts As Remaining Debt

The original OpenSpec task lists are mostly feature-complete.

The remaining debt is usually one of these:

- missing automated tests
- missing regression verification
- stale docs that were not yet synced to current code

Do not reopen a feature as "not implemented" unless current code and current Trellis specs both show the capability is actually absent.

---

## Quick Checklist

Before changing an existing phase-one flow:

- [ ] Search current controllers, pages, and API clients first.
- [ ] Read the matching Trellis feature doc.
- [ ] Check whether the remaining work is test debt instead of feature debt.
- [ ] If current code moved beyond legacy OpenSpec wording, update Trellis instead of reviving the old proposal text.
