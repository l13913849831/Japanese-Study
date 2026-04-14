## ADDED Requirements

### Requirement: Backend SHALL boot with documented platform conventions
后端 MUST 基于 Java 21 与 Spring Boot 3 建立可运行服务，并集成 Bean Validation、Spring Data JPA、Flyway、统一返回结构和集中式异常处理。

#### Scenario: Backend starts with an empty application runtime
- **WHEN** 开发者启动后端应用
- **THEN** 服务成功启动并加载统一的 Web、校验、持久化和迁移基础设施

#### Scenario: Validation failure is returned in the documented envelope
- **WHEN** 客户端提交不满足校验约束的请求
- **THEN** 后端返回包含 `success`、`data`、`error`、`timestamp` 的统一错误响应，并携带字段级校验详情

### Requirement: Backend SHALL align module boundaries with the existing schema and API document
后端 MUST 围绕 `word_set`、`word_entry`、`study_plan`、`card_instance`、`review_log`、`anki_template`、`md_template`、`export_job` 所代表的领域建立模块骨架，并使模块分组与接口文档中的资源分组一致。

#### Scenario: Developer navigates the backend codebase
- **WHEN** 开发者查看后端代码目录
- **THEN** 可以明确识别词库、学习计划、卡片、模板和导出等模块入口，以及各自关联的 controller、service、repository、entity、dto

### Requirement: Backend SHALL treat current Flyway migrations as the persistence baseline
后端 MUST 在启动时执行现有 `V1` 至 `V5` 迁移，并将默认模板种子数据视为数据库基线的一部分，而不是由应用层重复初始化。

#### Scenario: Fresh database receives the baseline schema
- **WHEN** 后端连接到一个空的 PostgreSQL 数据库并运行迁移
- **THEN** 数据库中会创建第一阶段所需表结构、约束、索引以及默认模板数据
