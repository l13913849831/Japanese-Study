# Component Guidelines

> Component composition and page patterns for the current React + Ant Design UI.

---

## Overview

The UI currently favors straightforward function components with Ant Design primitives. Feature page components compose reusable shared building blocks such as `PageHeader`, `PageSection`, and `StatusState`.

There is no custom design-system wrapper layer yet. Reuse is currently light and pragmatic.

---

## Component Structure

Typical component order:

1. third-party imports
2. React imports
3. internal imports
4. local types/interfaces
5. component implementation

Page components often follow this flow:

- initialize form and local state
- wire React Query queries/mutations
- define event handlers
- render stacked `PageSection` blocks

Examples:

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/shared/components/StatusState.tsx`

---

## Props Conventions

- Use TypeScript interfaces for component props.
- Keep props explicit and narrow; do not pass giant untyped config objects.
- For simple wrappers, extend `PropsWithChildren` when children are part of the API.
- Use `ReactNode` for extensibility slots such as `extra`.

Examples:

- `PageHeaderProps` uses `title`, `description`, and optional `extra`
- `PageSectionProps` extends `PropsWithChildren`
- `StatusStateProps` encodes mode as a string union

---

## Styling Patterns

- Global layout tokens and utility classes live in `src/styles.css`.
- Per-component styling is mostly inline via the `style` prop for small layout concerns.
- Ant Design theme tokens are configured centrally in `AppProviders`.
- Avoid introducing another styling system unless there is a clear repo-wide decision.

Current examples:

- `frontend/src/styles.css`
- `frontend/src/app/providers.tsx`
- `frontend/src/app/shell/AppShellLayout.tsx`

---

## Accessibility

- Prefer Ant Design form, table, button, and input components for built-in accessibility behavior.
- Use semantic page hierarchy with `Typography.Title` and descriptive labels in `Form.Item`.
- Keep button text explicit about the action.
- Preserve keyboard access when using clickable rows or menu navigation.

The current code relies heavily on Ant Design defaults, so any custom interactive element should match that baseline.

---

## Common Mistakes

- Do not embed shared API logic inside presentational shared components.
- Do not over-abstract one-off page layouts into generic wrappers too early.
- Do not use anonymous object blobs for props when a small typed interface is clearer.
- Do not move all text and UI state into global stores when it is page-local.

---

## Examples

- `frontend/src/shared/components/PageHeader.tsx`
- `frontend/src/shared/components/PageSection.tsx`
- `frontend/src/shared/components/StatusState.tsx`
- `frontend/src/features/word-sets/WordSetPage.tsx`
