# Directory Structure

> 基于当前 React + Vite 前端整理出的目录规范。

---

## 总览

前端结构现在非常稳定，主干就是三层：

- `app/`: 应用壳、路由、provider、query client
- `features/`: 业务页面和 feature-local API
- `shared/`: 跨 feature 复用的基础设施、组件和轻量 store

不要把这三层打散。新代码先判断属于哪一层，再落目录。

---

## 实际目录

```text
frontend/
  src/
    app/
      providers.tsx
      query-client.ts
      router.tsx
      shell/
    features/
      dashboard/
      word-sets/
      study-plans/
      cards/
      templates/
      export-jobs/
    shared/
      api/
      components/
      config/
      store/
    main.tsx
    styles.css
```

---

## 真实模式

### 1. feature 目录以页面为中心

每个 feature 目前至少有两类文件：

- `<FeatureName>Page.tsx`
- `api.ts`

示例：

- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/word-sets/api.ts`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
- `frontend/src/features/templates/api.ts`

### 2. app 只放全局装配

- `router.tsx` 定义全部路由
- `providers.tsx` 组装 Ant Design 和 React Query
- `query-client.ts` 放全局查询默认值
- `shell/AppShellLayout.tsx` 放应用布局和主菜单

### 3. shared 只放跨 feature 通用件

- `shared/api`: 传输层和错误对象
- `shared/components`: 页面公共组件
- `shared/config`: 环境配置
- `shared/store`: 少量跨页状态

---

## 新代码放哪里

### 新页面或新业务流程

- 新建 `features/<feature-name>/`
- 页面容器继续命名为 `XxxPage.tsx`
- feature 专属接口类型和请求函数放 `api.ts`

### 可复用组件

- 只有两个以上 feature 会复用，才移到 `shared/components`
- 仍只服务一个页面时，先留在 feature 内

### 全局能力

- 路由改动放 `app/router.tsx`
- Provider、主题、React Query 默认配置放 `app/`
- 环境变量解析放 `shared/config`

---

## 命名约定

- 目录用 kebab-case：`word-sets`、`study-plans`
- 组件文件用 PascalCase：`WordSetPage.tsx`
- hook 或 store 以 `use` 开头：`useUiStore.ts`
- feature 请求入口固定叫 `api.ts`
- `src` 内部导入统一走 `@/`

---

## 反模式

- 不要把业务页面扔进 `shared/components`
- 不要在 feature 文件里私自创建路由
- 不要跨 feature 直接互相引用内部实现
- 不要用一长串 `../../..` 代替 `@/`

---

## 参考文件

- `frontend/src/app/router.tsx`
- `frontend/src/app/providers.tsx`
- `frontend/src/app/shell/AppShellLayout.tsx`
- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/shared/components/PageHeader.tsx`
