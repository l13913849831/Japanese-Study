## Why

仓库当前已经具备第一阶段的产品方案文档、接口文档、数据库建模文档，以及 V1-V5 Flyway 迁移脚本，但仍缺少可运行的前后端工程骨架。现在先把 docs 与现有数据库基线收敛成统一的框架搭建方案，可以避免后续实现阶段出现接口、数据模型和工程结构三者脱节。

## What Changes

- 创建第一阶段可运行的项目基础骨架，包括 Spring Boot 后端与 React/Vite 前端。
- 建立与现有 Flyway 迁移脚本一致的后端模块边界、实体命名映射、统一返回结构和异常处理约定。
- 建立与接口文档一致的前端应用壳层、路由导航、API 访问层和通用页面状态约定。
- 补充本地开发所需的配置基线，覆盖 PostgreSQL、Flyway、CORS、前后端联调代理与启动说明。

## Capabilities

### New Capabilities
- `platform-bootstrap`: 定义第一阶段本地开发、运行配置和仓库级启动骨架。
- `backend-service-foundation`: 定义基于 Spring Boot、JPA、Flyway 的后端基础服务能力与模块骨架。
- `frontend-app-shell`: 定义基于 React、TypeScript、Vite 的前端应用壳层与模块入口。

### Modified Capabilities
- None.

## Impact

- Affected code: `backend/` 工程骨架、未来新增的 `frontend/` 工程骨架、公共配置与开发文档。
- Affected APIs: `/api` 下第一阶段模块的统一响应结构、错误结构、模块路由分组。
- Dependencies: Java 21、Spring Boot 3、Spring Data JPA、Flyway、PostgreSQL、React 18、TypeScript、Vite、Ant Design、TanStack Query、Zustand、React Router。
- Systems: 本地 PostgreSQL 开发环境、Flyway 迁移执行链路、前后端联调流程。
