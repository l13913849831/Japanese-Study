# Error Handling

> 当前后端 API 的异常传播和失败响应规范。

---

## 总览

这套后端已经形成统一失败链路：

1. controller 接请求
2. DTO 和参数注解做基础校验
3. service 抛 `BusinessException` 处理可预期业务错误
4. `GlobalExceptionHandler` 统一转成 `ApiResponse.failure(...)`

因此新代码要做的是接入这条链路，而不是各写各的错误格式。

---

## 错误类型

### 业务错误

- 用 `BusinessException`
- 必须带 `ErrorCode`
- 消息直接面向调用方，可读，不要塞堆栈或底层类名

当前 `ErrorCode`：

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `CONFLICT`
- `BAD_REQUEST`
- `IMPORT_ERROR`
- `TEMPLATE_RENDER_ERROR`
- `EXPORT_ERROR`
- `INTERNAL_ERROR`

### 框架错误

由全局异常处理器统一兜底：

- `MethodArgumentNotValidException`
- `BindException`
- `ConstraintViolationException`
- `MissingServletRequestParameterException`
- `DataIntegrityViolationException`
- 其他未捕获 `Exception`

---

## 分层规则

### controller

- 不要 try/catch 业务异常后手搓响应
- 只做 `@Valid`、`@Min`、`@Max`、`@RequestPart` 这类入口校验
- 成功时统一返回 `ApiResponse.success(...)`

示例：

- `WordSetController.create(...)`
- `WordSetController.list(...)`
- `WordEntryController.update(...)`

### service

- 资源不存在时抛 `BusinessException(ErrorCode.NOT_FOUND, ...)`
- 业务规则冲突时抛 `CONFLICT`
- 业务输入不合法时抛 `VALIDATION_ERROR`
- 底层 `IOException` 等受检异常，只有需要转成业务语义时才 catch

示例：

- `StudyPlanService.getEntity(...)`
- `StudyPlanService.validateEditable(...)`
- `WordEntryService.validateUpload(...)`
- `ExportJobService.create(...)`

### 全局映射

`GlobalExceptionHandler` 负责：

- 聚合字段错误列表
- 映射 HTTP status
- 保持统一 envelope

当前映射：

- `NOT_FOUND` -> `404`
- `CONFLICT` -> `409`
- `BAD_REQUEST` / `IMPORT_ERROR` / `TEMPLATE_RENDER_ERROR` / `EXPORT_ERROR` / `VALIDATION_ERROR` -> `400`
- 其他 -> `500`

---

## 响应格式

统一失败包体结构：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      { "field": "name", "message": "must not be blank" }
    ]
  },
  "timestamp": "2026-04-17T00:00:00Z"
}
```

规则：

- `error.code` 必须稳定，可被前端识别
- `error.message` 用于用户提示或调试提示
- `details` 只在字段或约束校验时提供

---

## 什么时候抛哪种错

- 参数缺失、格式错误：交给 Jakarta Validation / Spring 绑定异常
- 业务对象不存在：`NOT_FOUND`
- 资源状态不允许当前动作：`CONFLICT`
- 业务字段规则不通过：`VALIDATION_ERROR`
- 导入文件、模板渲染、导出文件等流程错误：对应专用 `ErrorCode`

---

## 反模式

- 不要在 controller 里 `catch (Exception)` 然后直接返回字符串
- 不要新增接口时绕过 `ApiResponse.failure(...)`
- 不要用裸 `RuntimeException` 表达业务错误
- 不要把数据库或文件系统原始异常消息直接透给前端
- 不要吞掉异常后返回成功

---

## 参考文件

- `backend/src/main/java/com/jp/vocab/shared/web/GlobalExceptionHandler.java`
- `backend/src/main/java/com/jp/vocab/shared/exception/BusinessException.java`
- `backend/src/main/java/com/jp/vocab/shared/exception/ErrorCode.java`
- `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
- `backend/src/main/java/com/jp/vocab/exportjob/service/ExportJobService.java`
