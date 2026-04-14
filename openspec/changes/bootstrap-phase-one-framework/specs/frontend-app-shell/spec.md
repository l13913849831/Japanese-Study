## ADDED Requirements

### Requirement: Frontend SHALL provide a navigable phase-one application shell
前端 MUST 基于 React、TypeScript 与 Vite 建立应用壳层，并提供覆盖词库、学习计划、今日卡片、模板和导出模块的基础路由与导航入口。

#### Scenario: User opens the phase-one web app
- **WHEN** 用户进入前端应用
- **THEN** 用户可以通过统一布局和导航切换第一阶段各个核心模块页面

### Requirement: Frontend SHALL use a centralized API access foundation
前端 MUST 提供统一的 HTTP client、查询状态管理和错误处理机制，并以接口文档定义的响应包装结构作为默认解析约定。

#### Scenario: API request fails with a documented error response
- **WHEN** 后端返回标准错误包装结构
- **THEN** 前端能够统一解析错误码、错误消息和字段级错误，并在页面层展示

### Requirement: Frontend SHALL include starter pages for the first-phase modules
前端 MUST 为词库列表与导入、学习计划创建与查看、今日卡片查询、模板管理与预览、导出查询等流程提供基础页面容器与公共状态组件，以支持后续逐步填充业务逻辑。

#### Scenario: Developer extends a module page
- **WHEN** 开发者在任一第一阶段模块上继续实现具体业务功能
- **THEN** 可以直接复用现有的路由入口、页面容器以及 loading、empty、error 等公共状态模式
