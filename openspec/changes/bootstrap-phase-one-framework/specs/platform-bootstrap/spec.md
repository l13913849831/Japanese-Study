## ADDED Requirements

### Requirement: Repository SHALL provide a runnable phase-one workspace baseline
仓库 MUST 同时提供第一阶段后端与前端的工程入口，并为开发者定义明确的安装、启动和本地联调命令。

#### Scenario: Developer bootstraps the workspace
- **WHEN** 开发者按照仓库文档执行后端与前端的初始化及启动命令
- **THEN** 开发者可以在同一仓库内启动后端服务和前端开发服务器

### Requirement: Local development configuration SHALL align with first-phase dependencies
系统 MUST 提供面向本地开发的配置基线，至少覆盖 PostgreSQL 连接、Flyway 执行、后端 CORS，以及前端访问 `/api` 的联调目标。

#### Scenario: Developer connects to local PostgreSQL
- **WHEN** 开发者提供本地数据库连接参数并启动后端
- **THEN** 后端能够执行现有 Flyway 迁移并进入可服务状态

#### Scenario: Frontend calls backend through the local integration path
- **WHEN** 前端在本地开发模式下访问第一阶段 API
- **THEN** 请求会通过约定的本地联调路径到达后端 `/api` 服务
