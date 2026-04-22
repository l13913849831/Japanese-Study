# Hook Guidelines

> 当前前端对 hooks 的使用边界和抽取时机。

---

## 总览

这个项目的现状不是“万物皆 hook”，而是：

- React 内建 hooks 正常使用
- React Query hooks 直接写在页面里
- 自定义 hook 很少
- 唯一常驻的项目级 hook 形态是 `useUiStore`

结论很简单：先写清楚页面逻辑，再决定要不要抽 hook。

---

## 当前稳定模式

### 页面内直接使用 React Query

- `useQuery` 负责读
- `useMutation` 负责写
- `useQueryClient().invalidateQueries(...)` 负责刷新缓存

示例：

- `WordSetPage.tsx`
- `StudyPlanPage.tsx`
- `StudyDashboardPage.tsx`

### 轻量共享状态走 Zustand hook

- `useUiStore` 只保存 `currentPlanId`
- store 很小，没有演变成全局业务仓库

---

## 什么时候抽自定义 hook

满足其中至少一个再抽：

- 同一段状态编排会复用到多个组件
- 页面组件已经被 query / mutation / side effect 淹没，阅读成本明显升高
- 可以输出一个清晰、稳定、强类型的接口

不满足时，继续留在页面里。

---

## 自定义 hook 约定

- 命名必须是 `useXxx`
- 默认先放所属 feature 内
- 返回值要明确，不返回无类型大杂烩
- 网络请求仍然调用 `features/*/api.ts` 或 `shared/api/http.ts`

---

## 查询键规则

- 用稳定数组做 query key
- 先放资源名，再放实体 id、分页和筛选条件
- 与 mutation 刷新范围保持一致

真实例子：

- `["wordSets"]`
- `["studyPlans"]`
- `["wordEntries", selectedWordSet?.id, page, pageSize, keyword, level, tag]`

---

## 条件查询

- 依赖某个 id 才能发请求时，用 `enabled`
- 若 queryFn 内部用了非空断言，必须和 `enabled` 成对出现

当前例子：

- `WordSetPage` 里的 `selectedWordSet!.id` + `enabled: Boolean(selectedWordSet?.id)`

---

## 反模式

- 不要把只用一次的页面逻辑硬抽成 hook
- 不要在 render 阶段直接调 API helper
- 不要写 `fetchWordSetsHook` 这种和现有命名不一致的名字
- 不要把本地 UI 状态、服务器状态、全局状态混成一个 hook

---

## 参考文件

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/app/query-client.ts`
- `frontend/src/shared/store/useUiStore.ts`
