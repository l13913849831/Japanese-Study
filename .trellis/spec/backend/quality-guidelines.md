# Quality Guidelines

> Backend code quality standards based on the current codebase and tooling.

---

## Overview

The current backend is intentionally simple: Spring Boot controllers, services, repositories, entities, and Flyway migrations. The main quality goals are consistency of contracts, explicit persistence mapping, and predictable service-layer validation.

There is no separate lint setup visible in the repo yet. Quality is enforced through compileability, API consistency, migration correctness, and review discipline.

---

## Forbidden Patterns

- Do not return plain entities directly from controllers. Use DTO/response records.
- Do not put database or business logic in controllers.
- Do not mutate schema through `ddl-auto`; keep it at `none` and use Flyway.
- Do not bypass the shared response envelope for API endpoints under `/api`.
- Do not add hidden cross-module coupling without moving the shared logic into `shared/` or a clearly owned module.

---

## Required Patterns

- use `@Transactional` at the service layer, with `readOnly = true` for read paths
- use explicit `@Table` and `@Column` mappings for persisted entities
- use Jakarta Validation on request DTOs and controller parameters where applicable
- use `BusinessException` + `ErrorCode` for expected rule violations
- use paged list responses via `PageResponse.from(...)` for list endpoints
- keep environment-driven settings in `application.yml` plus `@ConfigurationProperties` classes

---

## Testing Requirements

Current state:

- no backend test sources are present yet under `backend/src/test`

Expectation for new backend work:

- at minimum, run project compilation before finishing
- add focused tests when introducing non-trivial service logic, persistence edge cases, or exception mapping changes
- prioritize tests for:
  - service validation logic
  - repository query behavior
  - controller response envelope and status mapping
  - migration-sensitive persistence changes

---

## Code Review Checklist

- Are routes, DTOs, and response envelopes consistent with existing `/api` contracts?
- Are service methods the only place where transactions and business rules are orchestrated?
- If schema or persistence changed, is there a matching Flyway migration?
- Are entity/table/column names aligned with existing snake_case database conventions?
- Are failure paths mapped to the right `ErrorCode` and HTTP status?
- If filesystem or infra behavior changed, are env/config contracts documented in `application.yml` and config properties?

---

## Examples

- `backend/src/main/java/com/jp/vocab/wordset/controller/WordSetController.java`
- `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
- `backend/src/main/java/com/jp/vocab/shared/web/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
