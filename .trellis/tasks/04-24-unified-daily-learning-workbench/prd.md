# implement: unified daily learning workbench

## Goal

把当前分散的单词 dashboard 与知识点 dashboard 收敛成一个统一的今日学习工作台，让用户每天打开系统后能直接看到今天要做什么，并一键进入对应学习线。

## What I already know

* 当前 `/dashboard` 只展示单词学习线数据。
* 当前知识点线有独立 `/notes/dashboard`。
* 两条线都已有可复用聚合接口：
  * `GET /api/dashboard`
  * `GET /api/notes/dashboard`
* 单词线 dashboard 已能提供：
  * 今日总量
  * 活跃计划
  * 7 天趋势
* 知识点线 dashboard 已能提供：
  * 今日待复习
  * 总知识点
  * 已复习数
  * 掌握度分布
  * 7 天趋势
  * 最近新增知识点
* 当前复习入口仍分散：
  * 单词复习要先进入 `/cards`
  * 知识点复习要进入 `/notes/review`
* 单词复习页依赖 `useUiStore.currentPlanId` 做跨页带计划上下文。

## Assumptions (temporary)

* 这轮先做统一入口，不强行合并两条底层数据模型。
* 这轮优先复用已有 dashboard 接口，不新建后端聚合接口。
* `/dashboard` 直接升级成统一工作台，而不是再新增一个平行首页。

## Open Questions

* 当前没有阻塞问题，先按“复用现有接口 + 前端聚合展示”的最小可行方案实现。

## Requirements

* `/dashboard` 改为统一今日学习工作台。
* 工作台同时展示：
  * 单词学习线今日概览
  * 知识点学习线今日概览
  * 总体今日工作量摘要
  * 快速开始入口
* 用户应能从工作台一键进入：
  * 单词复习
  * 知识点复习
  * 单词计划明细
  * 知识点管理页
* 单词入口应尽量复用当前 `currentPlanId` 机制，减少重复输入。
* 原有独立 `/notes/dashboard` 可保留，作为知识点深度统计页。
* 当前工作台优先强调“今天做什么”，不是把两个旧 dashboard 原样拼接。

## Acceptance Criteria

* [ ] 打开 `/dashboard` 能同时看到单词与知识点两条线的今日任务摘要。
* [ ] 工作台能展示一个合并后的今日总览。
* [ ] 用户能从工作台直接进入单词复习和知识点复习。
* [ ] 用户能从工作台直接跳到单词计划/知识点管理相关页面。
* [ ] 原有查询失败、空数据状态在统一工作台里有明确反馈。
* [ ] `npm run build` 通过。

## Definition of Done

* 前端构建通过
* 工作台主路径可手工验证
* 路由与导航文案同步
* 不引入新的后端契约漂移

## Out of Scope

* 不改单词复习页和知识点复习页的核心交互流程
* 不新增统一后端聚合接口
* 不下掉原有 `/notes/dashboard`

## Technical Approach

* 复用 `StudyDashboardPage` 作为承载点，把它升级成统一工作台。
* 页面内同时发两个查询：
  * `getStudyDashboard(date)`
  * `getNoteDashboard(date)`
* 前端聚合一个统一 overview：
  * 今日总待处理
  * 单词待处理
  * 知识点待处理
  * 已完成量
* 保留原有单词活跃计划卡片，作为“单词学习线”细节区。
* 增加“知识点学习线”摘要区和最近新增知识点区。
* 顶部动作区提供统一入口：
  * 开始单词复习
  * 开始知识点复习
  * 打开知识点页

## Decision (ADR-lite)

**Context**:

项目当前已有两条学习线，但首页仍只偏向单词线，导致用户每天要自己判断该去哪做事。

**Decision**:

本轮把 `/dashboard` 升级成统一今日学习工作台，优先通过前端聚合现有 dashboard 接口实现。

**Consequences**:

* 优点：
  * 落地快
  * 风险低
  * 不新增后端耦合
* 缺点：
  * 工作台仍依赖两个分离接口
  * 后续如果需要更细的合并排序或跨线推荐，可能再补统一后端聚合接口

## Technical Notes

* 主要改动区域：
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
  * `frontend/src/app/shell/AppShellLayout.tsx`
  * `frontend/src/features/dashboard/api.ts`
  * 可能复用 `frontend/src/features/notes/api.ts`
* 相关现有页面：
  * `frontend/src/features/notes/NoteDashboardPage.tsx`
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
