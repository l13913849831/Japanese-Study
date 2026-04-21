# Implement Study Dashboard As Home

## Goal

新增一个面向学习者的看板首页，用统一入口展示“今天该学什么、哪些计划在推进、近 7 天学习趋势如何”，并将系统默认入口从词库管理切换为学习看板。

## What I already know

- 当前根路由 `/` 会直接跳转到 `/word-sets`，默认入口实际上是词库维护页，而不是学习首页。
- `frontend/src/features/word-sets/WordSetPage.tsx` 主要负责词集创建、导入、词条维护与筛选，更偏后台录入和数据准备。
- `frontend/src/features/study-plans/StudyPlanPage.tsx` 已支持学习计划创建、更新、模板选择和 `activate` / `pause` / `archive` 生命周期操作。
- `frontend/src/features/cards/TodayCardsPage.tsx` 已支持按计划和日期查询今日卡片、提交复习结果、查看复习历史。
- 后端已有单计划日历接口 `GET /api/study-plans/{planId}/cards/calendar`，但没有跨计划聚合总览接口。
- 当前菜单中没有 dashboard，看板需要新增独立页面和导航入口。

## Requirements

- 新增独立学习看板页面 `/dashboard`。
- 根路由 `/` 默认跳转到 `/dashboard`。
- 左侧菜单新增“学习看板”，并放在第一个导航位。
- 看板第一版按“标准版”范围实现：
  - 今日学习总览
  - 活跃计划摘要
  - 近 7 天新学 / 复习趋势
  - 多计划进度对比
- 后端提供支撑看板的聚合查询接口，避免前端通过多次单计划请求拼装。
- 前端看板页以只读展示为主，不在该页承载词库维护或模板编辑操作。
- 页面应明确区分“学习入口”和“数据管理入口”：`word-sets` 继续保留，但退回为资料维护页。

## Acceptance Criteria

- [ ] 访问 `/` 时默认进入 `/dashboard` 而不是 `/word-sets`
- [ ] 侧边栏存在“学习看板”菜单，且可以正常跳转
- [ ] 看板能展示今天的待学总量与活跃计划摘要
- [ ] 看板能展示近 7 天的新学 / 复习趋势
- [ ] 看板能展示多个计划之间的进度或负载对比
- [ ] 后端新增的聚合接口返回的数据足以支撑上述 4 个模块
- [ ] 页面在没有活跃计划或没有学习数据时有清晰空态
- [ ] 当前词库、计划、卡片、模板、导出页面导航不被破坏

## Technical Approach

- 后端新增一个 dashboard 聚合查询接口，集中返回：
  - 今日概览
  - 活跃计划摘要列表
  - 最近 7 天趋势
  - 计划对比数据
- 聚合查询优先复用已有 `study_plan`、`card_instance`、`review_log` 三类数据，而不是引入新表。
- 前端新增 `dashboard` feature 目录，封装页面、API 请求和展示组件。
- 路由层把根路径重定向改到 `/dashboard`，并把 dashboard 菜单放到左侧第一位。
- UI 方向以信息总览为主，强调可读性和快速决策，而不是做复杂交互。

## Decision (ADR-lite)

**Context**: 当前系统已具备学习闭环，但默认入口仍然是词库管理页，用户进入后看不到“今天学什么”和“最近学得怎样”的核心信息。

**Decision**: 将学习看板做成独立首页，并把它设为系统默认入口；首版采用“标准版”范围，而不是仅做极简摘要。

**Consequences**:

- 产品入口重心从数据录入转向学习执行与进度感知
- 需要新增跨层聚合接口，而不仅是调整前端路由
- 词库页仍然保留，但定位收缩为资料维护页

## Out of Scope

- 导入预校验和导入增强流程
- 导出模板选择和导出预览增强
- 月度 / 长期统计、连续打卡、阶段分布等更重的分析模块
- 看板上的词条编辑、模板编辑、计划编辑直达操作

## Technical Notes

- 重点涉及文件：
  - `frontend/src/app/router.tsx`
  - `frontend/src/app/shell/AppShellLayout.tsx`
  - `frontend/src/features/word-sets/WordSetPage.tsx`
  - `frontend/src/features/study-plans/StudyPlanPage.tsx`
  - `frontend/src/features/cards/TodayCardsPage.tsx`
  - `frontend/src/features/cards/api.ts`
  - `frontend/src/features/study-plans/api.ts`
  - `backend/src/main/java/com/jp/vocab/card/controller/CardQueryController.java`
  - `backend/src/main/java/com/jp/vocab/card/service/CardQueryService.java`
  - `backend/src/main/java/com/jp/vocab/card/service/CardReviewService.java`
  - `backend/src/main/java/com/jp/vocab/studyplan/controller/StudyPlanController.java`
- 当前后端只有单计划 `calendar` 查询，没有 dashboard 聚合接口，这是本任务的关键新增点。
- 如果需要文档同步，后续应回写：
  - `docs/api-specification.md`
  - `docs/system-usage-guide.md`
  - `docs/recommended-next-features.md`

## Implementation Plan

- PR1: 新增 dashboard 聚合接口与 DTO，打通最小可用数据返回
- PR2: 新增 `/dashboard` 页面与菜单入口，完成首页切换
- PR3: 补齐趋势展示、空态、文档同步与验收检查
