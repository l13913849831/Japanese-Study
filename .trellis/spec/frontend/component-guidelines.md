# Component Guidelines

> 基于当前页面实现总结出的组件编写规范。

---

## 总览

当前组件风格偏直接，不追求过度抽象。主线是：

- 页面组件负责状态编排和交互
- 共享组件保持薄、通用、无业务耦合
- 布局和视觉优先复用 Ant Design

---

## 页面组件模式

当前页面大多遵循这个顺序：

1. import
2. 局部类型和常量
3. 小型纯函数
4. 组件内部 state / query / mutation
5. handler
6. JSX

真实例子：

- `WordSetPage.tsx` 先定义 `parseTags`、`buildWordEntryPayload`
- `StudyPlanPage.tsx` 先定义 `fillForm`、`parseReviewOffsets`
- `ExportJobPage.tsx` 和 `TemplatePage.tsx` 也沿用“顶部纯函数 + 页面容器”的结构

---

## 共享组件模式

共享组件现在都很薄：

- `PageHeader`: 标题、说明、右侧扩展区
- `PageSection`: 基于 Antd `Card`
- `StatusState`: 统一 loading / empty / error

规则：

- 保持 props 小而明确
- 不把 React Query、fetch、业务 mutation 塞进共享组件
- 共享组件只承载展示层复用

---

## Props 约定

- 用 `interface` 定义 props
- 需要插槽时用 `ReactNode`
- 需要 children 时用 `PropsWithChildren`
- mode、status 这类有限集合优先用字符串联合类型

示例：

- `PageHeaderProps.extra?: ReactNode`
- `PageSectionProps extends PropsWithChildren`
- `StatusStateProps.mode: "loading" | "empty" | "error"`

---

## 样式约定

样式来源现在有三层：

- 全局类和基础布局在 `src/styles.css`
- 主题 token 在 `app/providers.tsx`
- 局部布局用少量内联 `style`

当前风格不是 CSS Modules，也没有引入额外样式方案。新增样式先沿用这三层，不要突然切 styled-components 或 Tailwind。

示例：

- `styles.css` 里的 `.page-stack`、`.table-toolbar`
- `providers.tsx` 里的 `colorPrimary`、`borderRadius`
- `PageHeader.tsx` 的 flex 布局内联样式

---

## 可访问性

- 表单输入优先用 `Form.Item` 提供 label 和规则
- 操作按钮文本要明确，不只写图标
- 状态组件沿用 Antd `Result`、`Empty`、`Spin`
- 页面层级优先用 `Typography.Title`

---

## 反模式

- 不要把 feature 专属业务组件提前抽进 `shared/components`
- 不要为了“通用”把 props 做成一个大 object bag
- 不要把表单转换逻辑塞进共享组件
- 不要把所有布局细节都写成巨型内联样式对象

---

## 参考文件

- `frontend/src/shared/components/PageHeader.tsx`
- `frontend/src/shared/components/PageSection.tsx`
- `frontend/src/shared/components/StatusState.tsx`
- `frontend/src/features/word-sets/WordSetPage.tsx`
- `frontend/src/features/study-plans/StudyPlanPage.tsx`
