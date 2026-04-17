# Quality Guidelines

> Frontend quality standards based on the current React/Vite implementation.

---

## Overview

The current frontend emphasizes clarity over abstraction. A feature page is allowed to be self-contained if it stays readable, while shared infrastructure is kept in `app/` and `shared/`.

There is no visible dedicated lint or test setup in the repository yet, so practical quality checks are build success, route behavior, API envelope compatibility, and UI consistency.

---

## Forbidden Patterns

- Do not call `fetch` directly from page components when `shared/api/http.ts` already defines the transport conventions.
- Do not bypass React Query for server-backed state.
- Do not place feature-specific DTOs in `shared/api/types.ts`.
- Do not introduce a second global state solution for small UI state.
- Do not break the `@/` alias convention with deep relative imports when importing from `src`.

---

## Required Patterns

- Use typed API wrapper functions in `features/*/api.ts`
- Use `useQuery`/`useMutation` for server interactions
- Surface loading, error, and empty states explicitly, usually with `StatusState`
- Keep top-level routing in `app/router.tsx`
- Keep app-wide providers in `app/providers.tsx`
- Keep theme and layout consistency with the existing Ant Design setup

---

## Testing Requirements

Current state:

- no frontend test files are present in `frontend/src`

Expectation for new work:

- run `npm run build` before finishing
- manually verify the affected route and main interaction path
- add tests once the repo gains a test runner or when logic becomes complex enough to justify introducing one

High-value future test targets:

- API helper error unwrapping
- form submit transformations
- route-level loading/error rendering

---

## Code Review Checklist

- Are route, page, and API wrapper boundaries consistent with `app / features / shared`?
- Is server state managed through React Query with stable query keys?
- Are shared components generic enough to belong in `shared/components/`?
- Do API payload/response types match backend envelope and DTO expectations?
- Are loading/error/empty states handled, not ignored?
- Does the change preserve the existing theme, layout, and import alias conventions?

---

## Examples

- `frontend/src/app/router.tsx`
- `frontend/src/features/word-sets/api.ts`
- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/shared/api/http.ts`
