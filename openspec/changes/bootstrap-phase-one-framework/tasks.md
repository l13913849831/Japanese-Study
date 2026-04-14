## 1. Workspace bootstrap

- [x] 1.1 初始化 `backend/` 工程骨架，补齐 Java 21、Spring Boot 3、Web、Validation、JPA、Flyway、PostgreSQL 依赖与启动入口
- [x] 1.2 初始化 `frontend/` 工程骨架，补齐 React 18、TypeScript、Vite、Ant Design、TanStack Query、Zustand、React Router 基础依赖
- [x] 1.3 补齐本地开发配置说明，覆盖 PostgreSQL 连接、Flyway 执行、CORS、前端 API 代理和基础启动命令

## 2. Backend foundation

- [ ] 2.1 配置数据源与 Flyway，验证现有 V1-V5 迁移可以在空库上成功执行
- [x] 2.2 实现统一返回结构、全局异常处理和参数校验错误映射，对齐 `docs/api-specification.md`
- [x] 2.3 按领域建立词库、学习计划、卡片、模板、导出模块的 controller/service/repository/entity/dto 骨架
- [x] 2.4 建立基于现有表结构的 DTO/VO 命名映射规则，并为后续接口实现预留模块入口

## 3. Frontend shell

- [x] 3.1 搭建前端应用壳层、基础布局、路由导航和模块级页面入口
- [x] 3.2 实现统一 HTTP client、查询 provider、错误处理和 API 服务封装基础设施
- [x] 3.3 为词库、学习计划、今日卡片、模板、导出模块创建列表/表单/查询类起始页面与公共状态组件

## 4. Verification and handoff

- [ ] 4.1 验证后端启动、Flyway 执行、前端启动以及本地 `/api` 联调链路
- [x] 4.2 记录工程结构、模块边界、运行命令和下一阶段功能实现建议
