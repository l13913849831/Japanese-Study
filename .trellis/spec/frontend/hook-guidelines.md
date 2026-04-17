# Hook Guidelines

> Hook usage patterns in the current frontend codebase.

---

## Overview

The codebase currently uses React built-in hooks plus TanStack Query hooks directly inside page components. There are almost no custom hooks yet; the only project-defined hook-style module is the small Zustand store `useUiStore`.

This is important: current convention prefers simple colocated query logic over premature hook extraction.

---

## Custom Hook Patterns

Current state:

- no feature-specific custom hooks are present in `src`
- shared state is exposed through `useUiStore`

When to add a custom hook:

- the same stateful/query workflow is reused by multiple components
- the page component becomes hard to read because state orchestration dominates rendering
- the hook can expose a stable, well-typed interface

If you add one:

- name it `useXxx`
- keep it in the owning feature unless reused across features
- return typed values and actions, not raw untyped bags

---

## Data Fetching

Server data is fetched with TanStack Query:

- `useQuery` for reads
- `useMutation` for writes
- `useQueryClient().invalidateQueries(...)` after successful mutations

API calls themselves live in `features/*/api.ts` or `shared/api/http.ts`, not inline in components.

Examples:

- `WordSetPage.tsx`: query list + import/create mutations
- `StudyPlanPage.tsx`: query lists + create/update mutations
- `app/query-client.ts`: shared query defaults

---

## Naming Conventions

- built-in and library hooks keep their original names (`useState`, `useEffect`, `useQuery`, `useMutation`)
- project-defined hooks/stores must start with `use`
- query keys should be stable arrays like `["wordSets"]` or `["wordEntries", id]`

Avoid inventing inconsistent names like `fetchWordSetsHook` or `wordSetStoreHook`.

---

## Common Mistakes

- Do not create a custom hook when the logic is used by only one page and remains readable inline.
- Do not call API helpers directly during render; wrap them in React Query.
- Do not use non-null assertions in query functions unless the query is guarded by `enabled`; `WordSetPage` is the current exception pattern and should stay tightly paired with `enabled`.
- Do not mix unrelated local UI state, server state, and cross-page state into a single hook.

---

## Examples

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/shared/store/useUiStore.ts`
- `frontend/src/app/query-client.ts`
