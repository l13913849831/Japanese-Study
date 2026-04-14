## Context

当前仓库已经沉淀了四类关键输入：

- `docs/japanese-vocab-system-proposal.md`：定义第一阶段产品目标、技术选型与核心业务方向。
- `docs/database-modeling.md`：定义实体关系、约束、字段语义与枚举值。
- `docs/api-specification.md`：定义 `/api` 下的统一返回结构、错误码和主要 DTO 形态。
- `backend/src/main/resources/db/migration/V1-V5__*.sql`：已经落地了词库、学习计划、卡片实例、模板、导出任务等数据库基线。

当前缺口也很明确：仓库还没有可运行的 Spring Boot 服务、没有前端工程、也没有把 docs 与 Flyway 汇总成统一的代码组织方式。因此本次变更不是直接交付全部业务功能，而是建立“第一阶段实现可以安全展开”的工程底座。

## Goals / Non-Goals

**Goals:**

- 建立与 docs 和现有 Flyway 一致的前后端工程骨架。
- 约束第一阶段模块边界，避免后续实现时出现实体、接口、页面入口各自演进。
- 固化本地开发约定，包括数据库连接、Flyway 执行、CORS、前端 API 代理与统一错误处理。
- 为后续词库、学习计划、卡片、模板、导出等功能提供可扩展的模块入口。

**Non-Goals:**

- 本次设计不要求一次性完成所有业务 CRUD 与导出逻辑。
- 不引入多用户、鉴权、`.apkg` 直出、动态记忆算法等超出第一阶段范围的能力。
- 不修改现有 V1-V5 Flyway 迁移所表达的数据基线，只围绕其建立应用层框架。

## Decisions

### 1. 以现有 Flyway 迁移作为持久层基线

选择直接把 `V1-V5` 迁移脚本视为当前数据库事实来源，后端实体、仓储、服务和 API 字段都围绕这套 schema 建立。

- Why: Flyway 已经落地，继续以 docs 为唯一真相会造成“文档一套、数据库一套”的分叉。
- Alternative considered: 重新从文档倒推一版 schema，再回头修改 SQL。该方案会放大返工范围，并且不利于尽快进入实现阶段。

### 2. 后端采用按领域分包的模块骨架

后端以 `word-set`、`study-plan`、`card`、`template`、`export-job` 五个纵向模块组织代码，每个模块拥有自己的 controller / service / repository / entity / dto。

- Why: 这与数据库表和接口分组天然对应，便于后续分阶段实现。
- Alternative considered: 按技术层分包（controller、service、repository 全局集中）。该方式在早期搭建快，但随着领域变多更容易交叉污染。

### 3. 前端先建特性路由壳层，再逐步填充业务页面

前端建立统一布局、导航与模块级页面容器，首批覆盖词库、学习计划、今日卡片、模板、导出几个主入口，并在页面层统一 loading / empty / error 状态。

- Why: 用户给出的目标是“第一次框架搭建”，因此先把信息架构和联调骨架立起来，比一次性深挖某个页面更适合。
- Alternative considered: 只创建一个空白首页。该方式虽然更轻，但后续仍需重新整理路由与共享基础设施。

### 4. API 契约严格对齐 `api-specification.md`

后端统一返回 `success/data/error/timestamp` 包装结构；校验错误和业务错误统一映射到文档中的错误码；前端 API client 以该包装结构为标准解析。

- Why: 这能保证文档、服务和前端调用方式在第一阶段一开始就统一。
- Alternative considered: 先返回裸 JSON，后续再包装。该方案会导致前后端很快产生一次破坏性重构。

### 5. 模板种子数据继续由 Flyway 管理

`Default Japanese Anki` 和 `Default Daily Markdown` 的默认模板继续由数据库迁移初始化，应用层只负责读取、查询和后续编辑。

- Why: 当前默认模板已经在 `V4__init_templates_seed.sql` 中表达，重复在代码里 seed 会造成双写风险。
- Alternative considered: 启动时由应用代码自动写入默认模板。该方式更灵活，但会与 Flyway 数据基线冲突。

## Risks / Trade-offs

- [框架范围过大] → 通过“骨架优先、业务渐进填充”的策略，只要求落下模块入口和公共基础设施，不在本次变更中完成所有业务细节。
- [文档与 SQL 仍可能存在细节偏差] → 实现时以现有 Flyway 为数据库事实来源，并在 DTO/VO 命名处建立 snake_case 到 camelCase 的清晰映射。
- [前后端同时起步导致联调阻塞] → 先固化共享 API 契约和错误包装，再让前端围绕 typed client 与占位页面并行推进。
- [后续功能变更可能重塑模块边界] → 采用纵向模块骨架，并在每个模块内保留扩展点，避免过早耦合。

## Migration Plan

1. 初始化后端工程，接入数据库配置、Flyway、统一返回和异常处理。
2. 初始化前端工程，建立路由、布局、HTTP client、查询与状态管理基础设施。
3. 以当前 Flyway 表为基线建立后端模块骨架和 DTO/VO 映射。
4. 以前端模块路由和页面壳层承接后续功能实现。
5. 通过本地 PostgreSQL 环境验证迁移执行、后端启动、前端代理联通。

Rollback strategy:

- 若框架实现中断，可保留 `openspec` 方案文件而不影响现有 SQL 资产。
- 若后端/前端骨架初始化结果不符合预期，可分别回退对应工程目录，不需要回滚 V1-V5 已有迁移文件。

## Open Questions

- 前端工程目录是否直接使用 `frontend/`，还是后续需要 monorepo/workspace 管理工具统一构建。
- 后端第一批是否同时补齐最小可用 smoke test，还是先完成基础骨架后再引入。
- 本地开发环境是否需要在本次变更内同步补 docker-compose，以进一步降低 PostgreSQL 启动成本。
