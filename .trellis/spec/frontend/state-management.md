# State Management

> How the current frontend separates local, global, and server state.

---

## Overview

The project uses three state categories:

- local component state with React hooks
- server state with TanStack Query
- small shared UI state with Zustand

There is no large global client-state framework such as Redux in the current codebase.

---

## State Categories

- Local UI state:
  - selected table row
  - last import result
  - current form fields
  - examples: `selectedWordSet`, `lastImportResult` in `WordSetPage`
- Server state:
  - paged lists and backend-backed resources
  - examples: `wordSetsQuery`, `studyPlanQuery`
- Shared client state:
  - small cross-page or shell-level UI state
  - example: `useUiStore.currentPlanId`
- Router state:
  - page identity and navigation are handled by `react-router-dom`

---

## When to Use Global State

Use Zustand only when state must outlive a single component or be shared without prop drilling.

Do not promote state to global just because several child elements in one page need it. Keep page-local state in the page container until there is a real reuse or persistence need.

---

## Server State

- Use TanStack Query for all backend-backed state.
- Central query defaults live in `app/query-client.ts`.
- Invalidate affected query keys after successful mutations.
- Keep fetch functions in API modules and let React Query own caching/retries/staleness.

Current defaults:

- `retry: 1`
- `staleTime: 30_000`
- `refetchOnWindowFocus: false`

---

## Common Mistakes

- Do not duplicate server state into Zustand or plain React state unless there is a clear reason.
- Do not store derivable form text globally.
- Do not bypass query invalidation after successful writes.
- Do not make `shared/store` a dumping ground for unrelated state.

---

## Examples

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/shared/store/useUiStore.ts`
- `frontend/src/app/query-client.ts`
