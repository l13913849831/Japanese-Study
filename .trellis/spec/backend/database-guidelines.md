# Database Guidelines

> 基于当前 PostgreSQL + Spring Data JPA + Flyway 实现整理出的持久层规范。

---

## 总览

当前数据库约定很明确：

- PostgreSQL 是主库
- Flyway SQL 管 schema
- JPA entity 做显式映射
- repository 负责简单查询
- service 负责事务和持久化编排

`application.yml` 里已经把 `spring.jpa.hibernate.ddl-auto` 设成 `none`。这意味着 schema 改动必须先写 migration，再改 entity。

---

## 表和列命名

- 表名用 snake_case 单数：`word_set`、`word_entry`、`study_plan`
- 列名用 snake_case：`word_set_id`、`daily_new_count`
- 外键约束：`fk_<table>_<target>`
- 唯一约束：`uk_<table>_<purpose>`
- 检查约束：`ck_<table>_<purpose>`
- 索引：`idx_<table>_<columns>`

真实例子：

- `uk_word_set_name`
- `fk_word_entry_word_set`
- `ck_study_plan_status`
- `idx_word_entry_word_set_source_order`

见：

- `backend/src/main/resources/db/migration/V1__init_word_set.sql`
- `backend/src/main/resources/db/migration/V2__init_study_plan.sql`

---

## Entity 规则

### 1. 显式映射，不吃隐式命名

- 每个 entity 都显式写 `@Table(name = "...")`
- 字段和库名不一致时显式写 `@Column(name = "...")`
- JSON 字段显式写 `columnDefinition = "jsonb"` 和 `@JdbcTypeCode(SqlTypes.JSON)`

示例：

- `StudyPlanEntity.reviewOffsets`
- `AnkiTemplateEntity.fieldMapping`
- `WordEntryEntity.wordSetId`

### 2. 审计字段走基类

- `created_at` / `updated_at` 统一放 `AuditableEntity`
- 新 entity 优先继承它，而不是每张表重复写时间戳逻辑

### 3. 状态变更优先放 entity 方法

- `StudyPlanEntity.activate() / pause() / archive()`
- `WordEntryEntity.update(...)`

这种写法让 service 管业务时序，entity 管自身可变状态。

---

## Migration 规则

### 新建或改表

- 一律新增 Flyway SQL 文件
- 文件名继续用 `V<number>__<description>.sql`
- 初始化建表沿用 `init_<domain>` 风格
- 性能或补充改动沿用 `add_<purpose>_indexes` 这类描述

当前序列：

- `V1__init_word_set.sql`
- `V2__init_study_plan.sql`
- `V3__init_card_instance.sql`
- `V4__init_templates_seed.sql`
- `V5__init_export_job.sql`
- `V6__add_phase_one_runtime_indexes.sql`

### Migration 内容

- 建表时把约束和必要索引一并写进去
- JSON 列要补上类型检查约束
- 状态字段要补 check constraint
- 不依赖 Hibernate 自动补全结构

---

## Repository 规则

- repository 继承 `JpaRepository`
- 简单存在性校验、删除、排序查询优先用派生方法
- 复杂业务过滤暂时可以留在 service 层，当前 `WordEntryService.list(...)` 就是现状
- 还没出现大量自定义 JPQL，新增前先确认是否真有必要

示例：

- `CardInstanceRepository.deleteByPlanId(Long planId)`
- `WordSetRepository.existsById(...)`
- `WordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(...)`

---

## 事务边界

- 事务写在 service，不写在 controller
- 读操作加 `@Transactional(readOnly = true)`
- 写操作用 `@Transactional`
- 同一次业务动作内需要多仓储协作时，保持在一个 service 方法里完成

示例：

- `WordSetService.list(...)`
- `StudyPlanService.create(...)`
- `ExportJobService.create(...)`

---

## 数据校验分层

- 基础请求格式校验放 DTO + Jakarta Validation
- 依赖资源存在性校验放 service
- 去重、状态合法性、跨字段约束放 service
- 数据库约束做最后一层兜底

典型例子：

- `StudyPlanService.validateRequest(...)`
- `WordEntryService.ensureNoDuplicate(...)`
- `GlobalExceptionHandler.handleDataIntegrityViolation(...)`

---

## 反模式

- 不要只改 entity 不写 migration
- 不要把事务放进 controller 或 repository
- 不要在 controller 里直接操作 repository
- 不要为了省事去掉显式 `@Table` / `@Column`
- 不要把可变入参集合直接挂到 entity 上，先复制，当前 `StudyPlanService` 已用 `List.copyOf(...)`

---

## 参考文件

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V1__init_word_set.sql`
- `backend/src/main/resources/db/migration/V2__init_study_plan.sql`
- `backend/src/main/java/com/jp/vocab/shared/persistence/AuditableEntity.java`
- `backend/src/main/java/com/jp/vocab/studyplan/entity/StudyPlanEntity.java`
- `backend/src/main/java/com/jp/vocab/template/entity/AnkiTemplateEntity.java`
