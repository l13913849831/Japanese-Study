# Directory Structure

> How frontend code is organized in the current React application.

---

## Overview

Frontend structure follows a simple three-way split:

- `app/`: application bootstrap, providers, router, shell
- `features/`: business-facing screens and their feature-local API wrappers
- `shared/`: reusable infrastructure and UI primitives used across features

Each feature currently owns a page component and an `api.ts` file.

---

## Directory Layout

```text
frontend/
  src/
    app/
      providers.tsx
      query-client.ts
      router.tsx
      shell/
    features/
      word-sets/
      study-plans/
      cards/
      templates/
      export-jobs/
    shared/
      api/
      components/
      config/
      store/
    main.tsx
    styles.css
```

---

## Module Organization

For a new feature:

- add a `features/<feature-name>/`
- keep the main page container in `<FeatureName>Page.tsx`
- add feature-scoped API types and request helpers in `api.ts`
- move reusable UI pieces to `shared/components/` only after they are shared by multiple features

Keep app wiring in `app/` rather than leaking routing or providers into feature directories.

---

## Naming Conventions

- directories are kebab-case: `word-sets`, `study-plans`, `export-jobs`
- React component files are PascalCase: `WordSetPage.tsx`, `AppShellLayout.tsx`
- shared state stores use `use*.ts`: `useUiStore.ts`
- feature-local API wrappers are named `api.ts`
- shared API utilities live under `shared/api`

Imports should use the `@/` alias for `src`.

---

## Examples

- `frontend/src/app/`: clean separation of router, providers, and query client
- `frontend/src/features/word-sets/`: representative feature module with page + API wrapper
- `frontend/src/shared/components/`: reusable layout/status primitives

---

## Anti-Patterns

- Do not put feature business screens in `shared/components/`.
- Do not import feature modules into other feature modules when `shared/` or API contracts should be the boundary.
- Do not bypass `app/router.tsx` with ad hoc routing setup inside feature files.
