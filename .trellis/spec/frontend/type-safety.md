# Type Safety

> TypeScript and API contract conventions for the current frontend.

---

## Overview

The frontend uses TypeScript in `strict` mode. Types are defined close to where they are used:

- shared transport-level types live in `shared/api/types.ts`
- feature-specific API payload and response shapes live in `features/*/api.ts`
- component props use local interfaces in component files

There is no dedicated runtime validation library in the frontend yet.

---

## Type Organization

- Put envelope and generic transport types in `shared/api/types.ts`
- Put feature DTOs next to the feature API wrapper in `features/<feature>/api.ts`
- Put component props next to the component
- Reuse exported feature types where screens and handlers share the same contract

Examples:

- `ApiEnvelope<T>` and `PageResponse<T>` in `shared/api/types.ts`
- `WordSet`, `WordEntry`, and payload types in `features/word-sets/api.ts`
- `StudyPlan` and `StudyPlanPayload` in `features/study-plans/api.ts`

---

## Validation

Current validation split:

- compile-time validation: TypeScript
- input/UI validation: Ant Design `Form.Item` rules
- backend contract validation: delegated to backend Jakarta Validation and business rules

Because there is no runtime schema library on the frontend yet, keep API helpers typed and assume backend envelope validation happens server-side.

---

## Common Patterns

- Use generic helpers for transport wrappers, for example `getJson<T>` and `ApiEnvelope<T>`
- Use string unions when a component API has a limited mode set, for example `StatusStateProps["mode"]`
- Use optional fields only when the backend can truly omit them
- Prefer explicit payload interfaces over inline object literals in mutation helpers

---

## Forbidden Patterns

- Avoid `any`
- Avoid broad `as` assertions for API payloads or query results
- Avoid duplicating the same DTO shape in multiple components
- Avoid bypassing strict null handling unless the code is guarded by control flow

Current exceptions to watch:

- `StudyPlanPage.handleSubmit(values: any)` should remain a temporary/simple pattern, not the default for new complex forms
- query functions using `selectedWordSet!.id` are acceptable only when paired with `enabled`

---

## Examples

- `frontend/tsconfig.json`
- `frontend/src/shared/api/types.ts`
- `frontend/src/shared/api/http.ts`
- `frontend/src/shared/components/StatusState.tsx`
