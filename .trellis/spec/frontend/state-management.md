# State Management

> 当前前端对本地状态、服务端状态和共享状态的划分。

---

## 总览

当前项目的状态分层很克制：

- 组件局部状态用 React state
- 服务端状态用 React Query
- 少量跨页状态用 Zustand
- 路由状态交给 React Router

没有 Redux，也没有把 React Query 数据复制进 store。

---

## 本地状态

适合放本地 state 的内容：

- modal 开关
- 当前编辑对象
- 当前筛选表单值
- 临时预览结果

真实例子：

- `WordSetPage` 的 `previewModalOpen`、`editingWordEntry`
- `WordSetPage` 的 `filters`
- `StudyPlanPage` 的表单选择和派生 UI 状态

---

## 服务端状态

所有后端资源继续走 React Query：

- 列表查询
- 明细查询
- 写操作后的刷新

全局默认值在 `app/query-client.ts`：

- `retry: 1`
- `staleTime: 30_000`
- `refetchOnWindowFocus: false`

这组默认值已经是项目基线，新查询先沿用，不要单独乱改。

---

## 共享客户端状态

只有确实跨页面或跨壳层共享时，才用 Zustand。

当前唯一示例：

- `useUiStore.currentPlanId`

这说明 store 目前只是轻量协调，不是业务真相源。

---

## 路由状态

- 当前页身份、菜单切换、默认重定向交给 `react-router-dom`
- 不要把路由选择再存一份到 Zustand 或普通 state

示例：

- `app/router.tsx`
- `AppShellLayout.tsx` 使用 `useLocation()` 和 `useNavigate()`

---

## 选择规则

- 只在当前页面用到：React state
- 来自后端：React Query
- 多页面共享但不值得进 URL：Zustand
- 与路径、页面身份相关：Router

---

## 反模式

- 不要把后端返回列表复制一份进 Zustand
- 不要把表单输入全搬到全局 store
- 不要写完 mutation 却忘了失效相关 query key
- 不要让 `shared/store` 变成杂物间

---

## 参考文件

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/app/query-client.ts`
- `frontend/src/app/router.tsx`
- `frontend/src/shared/store/useUiStore.ts`
