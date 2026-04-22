# Quality Guidelines

> 基于当前后端工程和构建方式整理出的质量要求。

---

## 总览

这个后端没有接入单独 lint 工具，质量门槛主要靠：

- 编译通过
- API 契约一致
- 事务和异常边界清楚
- migration 与 entity 同步
- 关键业务路径可读、可验证

---

## 必守模式

- controller 只返回 DTO / response record，不返回 entity
- `/api` 接口统一用 `ApiResponse`
- 分页列表统一用 `PageResponse`
- service 承担事务和业务规则
- entity 显式映射表和列
- 预期业务失败统一走 `BusinessException + ErrorCode`
- 配置项统一收在 `application.yml` 和 `@ConfigurationProperties`

---

## 禁止模式

- controller 直接调 repository
- controller 手写 try/catch 翻译业务异常
- 新增表结构却不写 Flyway migration
- 返回裸字符串、裸数组、裸 entity 破坏统一 envelope
- 偷偷跨模块复用内部实现，制造隐式耦合

---

## 代码评审清单

- controller 是否只保留入口职责
- service 是否承担完整业务编排
- 新增或修改持久化字段时，migration、entity、DTO 是否同步
- 错误码和 HTTP status 是否沿用现有映射
- 列表接口是否继续返回 `PageResponse`
- 新增 JSONB、状态字段、外键时，数据库约束是否完整
- 如果改了导入、导出、模板、学习流程，是否同步核对 `current-feature-contracts.md`

---

## 构建与检查

完成后至少跑：

```bash
cd backend
mvn test
```

当前项目虽然几乎没有测试源码，但 `mvn test` 仍然是最直接的编译和测试入口。

若只做快速校验，至少保证：

- Maven 编译成功
- Spring Boot 依赖解析正常
- 没有因为 DTO / entity / repository 变更引入编译错误

---

## 测试优先级

后端新增测试时，优先覆盖这些点：

- service 的校验和状态流转
- repository 的关键查询和约束行为
- 全局异常映射
- migration 相关持久化变更
- 导入预览、导出文件生成这类有边界条件的流程

当前高价值对象：

- `StudyPlanService`
- `WordEntryService`
- `ExportJobService`
- `GlobalExceptionHandler`

---

## 参考文件

- `backend/pom.xml`
- `backend/src/main/java/com/jp/vocab/wordset/controller/WordSetController.java`
- `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
- `backend/src/main/java/com/jp/vocab/shared/web/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
