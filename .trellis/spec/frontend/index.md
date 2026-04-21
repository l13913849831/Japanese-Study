# Frontend Development Guidelines

> Frontend conventions derived from the current React + Vite codebase.

---

## Overview

Frontend code lives under `frontend/` and uses:

- React 18
- TypeScript with `strict: true`
- Vite with `@` alias to `src`
- Ant Design for UI primitives
- TanStack Query for server state
- Zustand for small shared UI state
- plain CSS in `src/styles.css`

The project is structured around `app`, `features`, and `shared`.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Bootstrapped |
| [Component Guidelines](./component-guidelines.md) | Component patterns, props, composition | Bootstrapped |
| [Hook Guidelines](./hook-guidelines.md) | Custom hooks, data fetching patterns | Bootstrapped |
| [State Management](./state-management.md) | Local state, global state, server state | Bootstrapped |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, forbidden patterns | Bootstrapped |
| [Type Safety](./type-safety.md) | Type patterns, validation | Bootstrapped |
| [Current Feature Workflows](./current-feature-workflows.md) | Implemented phase-one routes and page workflows migrated from legacy OpenSpec | Active |

---

## Pre-Development Checklist

Read these files before changing frontend code:

- [Directory Structure](./directory-structure.md): when adding new pages, feature modules, or shared primitives
- [Component Guidelines](./component-guidelines.md): when building React components or page layouts
- [Hook Guidelines](./hook-guidelines.md): when introducing shared stateful logic or data-fetch abstractions
- [State Management](./state-management.md): when deciding between local state, React Query, Zustand, and URL/router state
- [Type Safety](./type-safety.md): when adding API contracts or component props
- [Current Feature Workflows](./current-feature-workflows.md): when touching implemented learner-facing routes, shared query keys, or page interaction flows
- [Quality Guidelines](./quality-guidelines.md): before finishing any frontend change

For cross-layer changes, also read [../guides/cross-layer-thinking-guide.md](../guides/cross-layer-thinking-guide.md).

---

## Representative Areas

- `app/`: providers, router, query client, shell layout
- `features/`: page-level business modules and API wrappers
- `shared/api/`: HTTP helpers, envelope types, error class
- `shared/components/`: reusable page primitives and status states
- `shared/store/`: lightweight shared Zustand state
