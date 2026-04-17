# Directory Structure

> How backend code is organized in the current Spring Boot application.

---

## Overview

Backend code is split by domain package first, then by responsibility inside that package. Shared cross-cutting code is placed under `com.jp.vocab.shared`.

The dominant structure today is:

- `controller`: HTTP entry points
- `service`: business logic and orchestration
- `repository`: Spring Data JPA repositories
- `entity`: JPA persistence models
- `dto`: request and response records
- `shared`: reusable infrastructure and contracts

---

## Directory Layout

```text
backend/
  src/main/java/com/jp/vocab/
    JapaneseVocabApplication.java
    wordset/
      controller/
      service/
      repository/
      entity/
      dto/
    studyplan/
      controller/
      service/
      repository/
      entity/
      dto/
    card/
      controller/
      service/
      repository/
      entity/
      dto/
    template/
      controller/
      service/
      repository/
      entity/
      dto/
    exportjob/
      controller/
      service/
      repository/
      entity/
      dto/
    shared/
      api/
      config/
      csv/
      exception/
      persistence/
      template/
      web/
  src/main/resources/
    application.yml
    db/migration/
```

---

## Module Organization

Create new business capabilities as domain packages alongside `wordset`, `studyplan`, and `exportjob`, not as giant horizontal buckets.

Within a domain package:

- put request/response contracts in `dto/`
- put HTTP mapping and parameter validation in `controller/`
- put orchestration, validation, and transaction boundaries in `service/`
- put JPA interfaces in `repository/`
- put database-mapped objects in `entity/`

Put cross-domain code in `shared/` only if at least two modules depend on it or it defines a stable application-wide contract.

---

## Naming Conventions

- package names are lowercase and domain-oriented: `wordset`, `studyplan`, `shared`
- controllers end with `Controller`
- services end with `Service`
- repositories end with `Repository`
- entities end with `Entity`
- API contracts end with `Request`, `Response`, or a domain-specific record name
- shared API wrappers live under `shared.api`

Prefer singular nouns for entities (`WordSetEntity`) and plural resource names for HTTP routes (`/api/word-sets`).

---

## Examples

- `backend/src/main/java/com/jp/vocab/wordset/`: complete CRUD-style module layout with controller/service/repository/entity/dto
- `backend/src/main/java/com/jp/vocab/studyplan/`: module with validation plus orchestration across repositories and services
- `backend/src/main/java/com/jp/vocab/shared/`: shared API envelope, exception mapping, config properties, and utility helpers

---

## Anti-Patterns

- Do not put business logic in controllers. `WordSetController` delegates directly to services.
- Do not create generic `util` packages inside each feature without a clear shared need.
- Do not skip the domain package structure by placing unrelated classes directly under `com.jp.vocab`.
