# Quality Guidelines

> 基于当前 React/Vite 工程整理出的前端质量要求。

---

## 总览

前端现在没有单独测试框架和 ESLint 配置，质量主要靠：

- TypeScript 构建通过
- 路由和页面边界清楚
- 服务端状态管理一致
- 交互路径手工验证
- API 契约不漂移

---

## 必守模式

- 请求统一走 `shared/api/http.ts`
- feature 请求函数统一放 `features/*/api.ts`
- 后端资源状态统一走 React Query
- 共享组件保持通用，不掺业务请求
- 顶层路由统一放 `app/router.tsx`
- `src` 内部导入统一走 `@/`

---

## 禁止模式

- 页面里直接写裸 `fetch`
- 把 feature DTO 塞进 `shared/api/types.ts`
- 绕开 React Query 手搓一套缓存
- 同时引入第二套全局状态方案
- 新建共享组件却内嵌业务接口调用

---

## 构建与检查

完成后至少跑：

```bash
cd frontend
npm run build
```

这个命令会先跑 `tsc -b`，再跑 `vite build`，是当前仓库最直接的类型和打包校验。

---

## 手工验证重点

- 受影响路由能正常进入
- loading / empty / error 三种状态至少覆盖到主路径
- 提交表单后提示文案和数据刷新正常
- mutation 后相关 query key 已刷新
- 如果改了布局或导航，桌面和窄屏都要看一眼

---

## 评审清单

- 页面、API、共享组件边界是否仍是 `app / features / shared`
- query key 是否稳定且和失效范围匹配
- 类型是否沿用共享 envelope 和 feature DTO
- 页面是否显式处理错误提示
- 新抽出来的共享组件是否真的跨 feature 可复用
- 样式是否继续沿用现有 token、全局类和少量内联布局

---

## 当前高价值检查点

- `WordSetPage` 这类大页面的 mutation 链路是否仍可读
- `StudyPlanPage` 的表单转换是否和后端 payload 一致
- `shared/api/http.ts` 的错误拆包是否被破坏
- `app/router.tsx` 的路由入口是否和菜单一致

---

## 参考文件

- `frontend/package.json`
- `frontend/src/app/router.tsx`
- `frontend/src/app/query-client.ts`
- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/word-sets/api.ts`
- `frontend/src/shared/api/http.ts`
