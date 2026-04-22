# Directory Structure

> 基于当前 Spring Boot 代码组织总结出的后端目录规范。

---

## 总览

后端主线是按业务域拆包，再在域内按职责分层。现在的稳定边界是：

- `controller/`: HTTP 入口，只做参数接收、注解校验、调用 service、返回统一 envelope
- `service/`: 事务、业务规则、跨仓储编排、外部依赖协调
- `repository/`: Spring Data JPA 查询接口
- `entity/`: 数据库映射对象和少量领域状态变更方法
- `dto/`: 请求和响应 record
- `shared/`: 跨域复用的基础设施和全局契约

---

## 实际目录

```text
backend/
  src/main/java/com/jp/vocab/
    JapaneseVocabApplication.java
    wordset/
      controller/
      dto/
      entity/
      repository/
      service/
    studyplan/
      controller/
      dto/
      entity/
      repository/
      service/
    card/
      controller/
      dto/
      entity/
      repository/
      service/
    template/
      controller/
      dto/
      entity/
      repository/
      service/
    exportjob/
      controller/
      dto/
      entity/
      repository/
      service/
    dashboard/
      controller/
      dto/
      service/
    shared/
      api/
      config/
      csv/
      exception/
      persistence/
      template/
      web/
  src/main/resources/
    application.yml
    db/migration/
```

---

## 真实模式

### 1. 先按业务域，再按分层

- `wordset`、`studyplan`、`template`、`exportjob` 都是完整域包
- `dashboard` 是只读聚合场景，所以只有 `controller / dto / service`
- 不存在全局 `controllers/`、`services/` 这种横向大桶

示例：

- `backend/src/main/java/com/jp/vocab/wordset/`
- `backend/src/main/java/com/jp/vocab/studyplan/`
- `backend/src/main/java/com/jp/vocab/exportjob/`

### 2. controller 保持很薄

- `WordSetController` 只接请求、做注解校验、回 `ApiResponse.success(...)`
- `WordEntryController` 把单条词条更新和删除独立成 item resource 路由
- controller 不拼业务状态，不手写异常翻译

示例：

- `backend/src/main/java/com/jp/vocab/wordset/controller/WordSetController.java`
- `backend/src/main/java/com/jp/vocab/wordset/controller/WordEntryController.java`
- `backend/src/main/java/com/jp/vocab/dashboard/controller/StudyDashboardController.java`

### 3. service 持有事务和业务规则

- `WordEntryService` 负责导入校验、去重、预览和落库
- `StudyPlanService` 负责生命周期流转、依赖资源校验、卡片重建触发
- `ExportJobService` 负责文件生成和导出任务持久化

### 4. shared 只放稳定通用能力

- `shared/api`: `ApiResponse`、`PageResponse`、`ApiError`
- `shared/web`: 全局异常处理、健康检查
- `shared/config`: `@ConfigurationProperties` 和 Web 配置
- `shared/persistence`: `AuditableEntity`
- `shared/csv`、`shared/template`: 通用解析和模板能力

---

## 新代码放哪里

### 新增业务能力

- 先判断属于哪个业务域
- 优先放进现有域包，比如词库相关继续放 `wordset/`
- 只有确实是新的一级业务边界，才新增同级域目录

### 新增 HTTP 接口

- 放在所属域的 `controller/`
- 列表和集合动作优先挂在拥有者资源上
- 单项编辑可拆到独立 item resource controller，当前 `WordSetController` + `WordEntryController` 就是这个模式

### 新增 DTO

- 请求体、响应体都放域内 `dto/`
- 命名继续用 `CreateXxxRequest`、`UpdateXxxRequest`、`XxxResponse`
- 导入、预览这种特殊流程可以用更具体的名词 record，比如 `WordEntryImportPreviewResponse`

### 新增公共能力

- 至少两个域要复用，或者它本身是应用级契约，才进 `shared/`
- 还只有单一业务使用时，不要提前抽到 `shared/`

---

## 命名约定

- 包名小写，按业务语义命名：`wordset`、`studyplan`
- 类名按职责后缀：
  - `*Controller`
  - `*Service`
  - `*Repository`
  - `*Entity`
  - `*Request`
  - `*Response`
- 路由资源名用复数 kebab-case：`/api/word-sets`、`/api/export-jobs`
- 实体名保持单数：`WordSetEntity`、`ExportJobEntity`

---

## 反模式

- 不要把业务规则塞进 controller
- 不要在业务域里随手建 `util`、`common`、`helper` 桶目录
- 不要把只服务一个模块的类提前丢进 `shared/`
- 不要绕过域目录，直接把业务类放到 `com.jp.vocab` 根下

---

## 参考文件

- `backend/src/main/java/com/jp/vocab/wordset/controller/WordSetController.java`
- `backend/src/main/java/com/jp/vocab/wordset/controller/WordEntryController.java`
- `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
- `backend/src/main/java/com/jp/vocab/shared/api/ApiResponse.java`
- `backend/src/main/java/com/jp/vocab/shared/persistence/AuditableEntity.java`
