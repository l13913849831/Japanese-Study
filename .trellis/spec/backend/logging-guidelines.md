# Logging Guidelines

> 当前项目的后端日志现状和后续新增日志时的约束。

---

## 现状

当前业务代码几乎没有显式应用日志，只有 `application.yml` 里保留了基础日志级别：

- `logging.level.org.flywaydb=info`
- `logging.level.org.springframework.web=info`

这说明两件事：

- 代码库现在不是“到处打日志”的风格
- 新日志只能补在真正有运维价值的边界上

---

## 什么时候该加日志

- 文件系统副作用：导出文件生成、目录创建失败
- 批量处理：CSV / APKG 导入、卡片批量生成
- 外部依赖或基础设施异常：文件读写、模板渲染、解析失败
- 启动或配置异常：环境变量、目录路径、跨域配置等

优先候选点：

- `ExportJobService.create(...)`
- `WordEntryService.importEntries(...)`
- `WordEntryService.previewImport(...)`
- `CardGenerationService.regenerateForPlan(...)`

---

## 什么时候不要加

- 普通 CRUD 成功路径，不要每个请求都打一条 `INFO`
- 已经会被统一返回给前端的业务校验失败，不要额外打印一份噪音日志
- 不要记录上传文件原文、模板全文、超长用户输入
- 不要打印数据库密码、Token、目录凭据等敏感信息

---

## 级别约定

- `INFO`: 关键流程开始/完成，且需要线上追踪
- `WARN`: 可恢复异常、被跳过的数据、降级处理
- `ERROR`: 未预期失败或基础设施故障
- `DEBUG`: 本地排查用，不作为常规依赖

---

## 日志内容

现在还没有自定义 structured logger。若新增日志，继续用标准 SLF4J 风格，但消息内容要带稳定上下文：

- 操作名：`preview import`、`create export`
- 关键 ID：`wordSetId`、`planId`、`exportJobId`
- 结果：`success`、`failed`、`skipped`
- 聚合数量：总数、成功数、重复数、错误数

建议格式：

- `"preview import started, wordSetId={}"`
- `"export file generated, planId={}, fileName={}"`
- `"import preview failed, wordSetId={}, reason={}"`

---

## 与异常处理的边界

- 业务规则失败通常不额外记日志，交给统一错误响应即可
- 基础设施异常在转成 `BusinessException` 前，可以记一条 `WARN` 或 `ERROR`
- 不要同时在每层都打一遍同一个异常

---

## 反模式

- 不要为每个 controller 方法都机械加开始/结束日志
- 不要在循环里逐行 `INFO`
- 不要把完整请求体或导入文件内容打到日志
- 不要在预期的 `BusinessException` 分支里刷堆栈

---

## 参考文件

- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/jp/vocab/exportjob/service/ExportJobService.java`
- `backend/src/main/java/com/jp/vocab/wordset/service/WordEntryService.java`
