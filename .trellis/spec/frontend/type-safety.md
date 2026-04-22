# Type Safety

> 当前前端的 TypeScript、API 契约和类型组织规范。

---

## 总览

前端启用了 `strict` 模式，类型组织原则是“类型跟着边界走”：

- 传输层通用类型放 `shared/api/types.ts`
- feature DTO 放各自 `features/*/api.ts`
- 组件 props 写在组件文件旁边

当前没有引入 zod 之类的运行时 schema 库，所以类型安全主要靠：

- TypeScript 编译期
- Antd 表单规则
- 后端统一 envelope 和校验

---

## 类型放置规则

### 共享传输层类型

放这里：

- `ApiEnvelope<T>`
- `PageResponse<T>`
- `ApiError`
- `ApiFieldError`

文件：

- `frontend/src/shared/api/types.ts`

### Feature DTO

接口类型和请求载荷放 feature 的 `api.ts`：

- `WordSet`
- `WordEntry`
- `StudyPlan`
- `WordEntryImportPreviewResult`

### 组件 props

- 就近定义
- 不要把组件 props 提前堆进一个全局 `types.ts`

---

## API helper 泛型规则

`shared/api/http.ts` 已经提供统一泛型 helper：

- `getJson<T>`
- `postJson<TResponse, TRequest>`
- `putJson<TResponse, TRequest>`
- `deleteJson<TResponse>`
- `postFormData<TResponse>`

新增 API 函数优先复用这些 helper，不要重新写 fetch 包装。

---

## 空值和可选字段

- 后端可能缺省返回时，字段才用 `?`
- 表单提交时，空字符串转 `undefined` 的转换写在页面或 feature helper 里
- 非空断言只在有控制流保护时使用

真实例子：

- `buildWordEntryPayload(...)` 把空值规范化为 `undefined`
- `selectedWordSet!.id` 依赖 `enabled`
- `ApiEnvelope<T>` 的 `data` 明确允许 `null`

---

## 联合类型和常量

有限状态优先用联合类型或精确定义的 record：

- `StatusStateProps["mode"]`
- `StudyPlanStatus`
- `WordEntryImportPreviewRow["status"]`
- `Record<StudyPlanStatus, string>`

这样写比到处散落裸字符串更稳。

---

## 现阶段例外

当前代码里有少量应避免扩散的简单写法：

- `StudyPlanPage` 的表单值类型仍比较宽松
- 个别 queryFn 里存在受 `enabled` 保护的非空断言

这些可以暂时接受，但不应成为新代码默认模板。

---

## 反模式

- 不要用 `any`
- 不要在多个组件里重复定义同一份 DTO
- 不要用大范围 `as` 强转吞掉类型问题
- 不要让组件直接依赖未封装的后端 envelope 细节

---

## 参考文件

- `frontend/tsconfig.json`
- `frontend/src/shared/api/types.ts`
- `frontend/src/shared/api/http.ts`
- `frontend/src/features/word-sets/api.ts`
- `frontend/src/features/study-plans/api.ts`
