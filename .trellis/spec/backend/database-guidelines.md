# Database Guidelines

> Persistence, schema, and query conventions for the current PostgreSQL + JPA stack.

---

## Overview

The project uses:

- PostgreSQL as the primary database
- Flyway SQL migrations under `backend/src/main/resources/db/migration`
- Spring Data JPA repositories for persistence access
- explicit JPA annotations for table/column mapping

Runtime schema changes are managed by Flyway. `spring.jpa.hibernate.ddl-auto` is set to `none`, so schema must not be created implicitly from entities.

---

## Query Patterns

- use `JpaRepository` as the default repository abstraction
- keep common list endpoints simple by using `findAll(PageRequest...)`, as seen in `WordSetService`, `StudyPlanService`, and `ExportJobService`
- put transaction boundaries at the service layer with `@Transactional` and `@Transactional(readOnly = true)`
- use repository-derived methods for simple deletes or existence checks, for example `CardInstanceRepository.deleteByPlanId(...)` and `WordSetRepository.existsById(...)`
- prefer entity factory or update methods over setting fields from controllers

Current examples:

- `backend/src/main/java/com/jp/vocab/wordset/service/WordSetService.java`
- `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
- `backend/src/main/java/com/jp/vocab/card/repository/CardInstanceRepository.java`

---

## Migrations

- add new migrations as versioned SQL files: `V<number>__<description>.sql`
- keep DDL explicit in SQL; do not rely on Hibernate auto-generation
- define constraints and indexes in the migration where the table is introduced
- follow the existing sequence of additive migrations such as:
  - `V1__init_word_set.sql`
  - `V2__init_study_plan.sql`
  - `V6__add_phase_one_runtime_indexes.sql`

Migration naming patterns in the current codebase:

- table creation: `init_<domain>`
- follow-up performance changes: `add_<purpose>_indexes`

---

## Naming Conventions

- tables use snake_case singular names: `word_set`, `study_plan`, `export_job`
- columns use snake_case: `word_set_id`, `daily_new_count`, `created_at`
- indexes use `idx_<table>_<columns>`
- unique constraints use `uk_<table>_<purpose>`
- foreign keys use `fk_<table>_<target>`
- check constraints use `ck_<table>_<purpose>`
- Java entities use camelCase fields with explicit `@Column(name = "...")`

JSON columns are stored as `jsonb` and mapped with `@JdbcTypeCode(SqlTypes.JSON)`:

- `StudyPlanEntity.reviewOffsets`
- `AnkiTemplateEntity.fieldMapping`

---

## Common Mistakes

- Do not introduce schema changes only in entities. Every schema change must have a Flyway migration.
- Do not rely on implicit naming when the database contract is snake_case. Keep explicit `@Table` and `@Column` mappings.
- Do not store mutable list inputs directly; copy them before persistence, as `StudyPlanService` does with `List.copyOf(...)`.
- Do not bypass referential validation when business rules depend on foreign IDs. Validate dependent IDs in the service layer before saving.

---

## Examples

- `backend/src/main/resources/db/migration/V1__init_word_set.sql`
- `backend/src/main/resources/db/migration/V2__init_study_plan.sql`
- `backend/src/main/java/com/jp/vocab/studyplan/entity/StudyPlanEntity.java`
- `backend/src/main/java/com/jp/vocab/template/entity/AnkiTemplateEntity.java`
