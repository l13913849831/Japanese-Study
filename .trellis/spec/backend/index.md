# Backend Development Guidelines

> Backend conventions derived from the current Spring Boot codebase.

---

## Overview

Backend code lives under `backend/` and uses:

- Spring Boot 3.3 + Java 21
- Spring MVC for HTTP APIs
- Jakarta Validation for request validation
- Spring Data JPA + PostgreSQL
- Flyway for schema migrations

The current codebase is organized by domain package (`wordset`, `studyplan`, `card`, `template`, `exportjob`) plus a `shared` package for cross-cutting concerns.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Bootstrapped |
| [Database Guidelines](./database-guidelines.md) | ORM patterns, queries, migrations | Bootstrapped |
| [Error Handling](./error-handling.md) | Error types, handling strategies | Bootstrapped |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, forbidden patterns | Bootstrapped |
| [Logging Guidelines](./logging-guidelines.md) | Structured logging, log levels | Bootstrapped |

---

## Pre-Development Checklist

Read these files before changing backend code:

- [Directory Structure](./directory-structure.md): when creating or extending a backend module
- [Database Guidelines](./database-guidelines.md): when touching entities, repositories, Flyway SQL, or persistence contracts
- [Error Handling](./error-handling.md): when adding validations, business rules, or exception paths
- [Logging Guidelines](./logging-guidelines.md): when adding diagnostics or operational visibility
- [Quality Guidelines](./quality-guidelines.md): before finishing any backend change

For cross-layer changes, also read [../guides/cross-layer-thinking-guide.md](../guides/cross-layer-thinking-guide.md).

---

## Representative Modules

- `wordset`: create/list word sets and import words
- `studyplan`: create/update study plans and regenerate cards
- `template`: template listing and preview
- `exportjob`: export generation and file download
- `shared`: API envelope, config properties, exceptions, and helper utilities
