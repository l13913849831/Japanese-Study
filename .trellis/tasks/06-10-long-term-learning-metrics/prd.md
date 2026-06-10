# 长周期学习指标

## Goal

补齐当前工作台只覆盖当天和近 7 天的短板，让用户能看清长期学习稳定性、复习负载变化、单词线与知识点线的长期趋势。

## What I already know

- 当前没有正在进行的 Trellis 任务。
- `docs/open-items.md` 把长周期学习指标列为真实未做的产品增强项。
- 现有 `/api/dashboard` 返回单词线当天概览、激活计划摘要和近 7 天趋势。
- 现有 `/api/notes/dashboard` 返回知识点线当天概览、掌握度分布、近 7 天趋势和最近知识点。
- 单词线历史数据来自 `review_log`、`card_instance`、`study_plan`。
- 知识点线历史数据来自 `note_review_log`、`note`、`note_source`。
- 当前不需要新建明细数据表，优先按历史日志和当前 FSRS 状态做聚合。

## Assumptions

- 第一版只做只读聚合，不改复习调度。
- 第一版服务个人用户，不做管理员全局学习统计。
- 时间粒度优先支持 30 / 90 / 180 天。
- 长期趋势先放在 `/dashboard`，不单独新增顶层菜单。

## Requirements

### MVP Scope

- 增加当前连续学习天数和最长连续学习天数。
- 增加近 7 天、近 30 天复习完成量。
- 增加 90 天单词 / 知识点 / 总复习量按天趋势。
- 增加未来 7 / 14 / 30 天单词与知识点到期负载预测。
- 在 `/dashboard` 展示长期摘要、90 天趋势和未来负载。

### Full Scope Backlog

- 增加连续学习天数指标，按单词或知识点任意一条线当天有复习记录算学习日。
- 增加周 / 月复习量，区分单词卡片和知识点复习。
- 增加单词 / 知识点分线趋势，支持按天聚合并覆盖 30 / 90 / 180 天。
- 增加掌握度分布快照，复用单词 FSRS 状态和知识点 `mastery_status`。
- 增加未来负载预测，展示未来 7 / 14 / 30 天的到期量。
- 工作台展示长期摘要卡、趋势图和未来负载提示。
- 支持按计划筛选单词线长期趋势。
- 支持按知识点标签筛选知识点线长期趋势。
- 支持周粒度和月粒度聚合视图。
- 支持完成率指标：已完成量 / 到期量。
- 支持平均响应耗时、评分分布、`AGAIN` 比例。
- 支持薄弱项新增 / 消除趋势。
- 支持掌握度阶段迁移：未开始、学习中、巩固中、已掌握。
- 支持长期负载过载阈值配置和提示文案。
- 支持把长期报告导出到 Markdown 复盘材料。
- 支持管理员全局匿名聚合统计，作为后续独立后台能力。

## Acceptance Criteria

- [x] 后端能返回长期学习摘要，包括当前连续学习天数、最长连续学习天数、近 7 / 30 天完成量。
- [x] 后端能返回按天聚合的单词复习量、知识点复习量和总复习量。
- [x] 后端能返回未来 7 / 14 / 30 天的单词与知识点到期量。
- [x] 前端 `/dashboard` 能展示长期摘要、分线趋势和未来负载。
- [x] 无历史数据时，页面显示可理解的空状态。
- [x] 聚合结果只包含当前登录用户的数据。
- [x] 后端补充 service/controller 回归测试。
- [x] 前端至少通过 build/type-check。

## Definition of Done

- Tests added/updated where behavior changes.
- Maven test passes with JDK 21.
- Frontend build passes.
- API docs and Trellis specs updated.
- `docs/open-items.md` 移除或下调本项。

## Out of Scope

- 不做 Keycloak、微信登录、多端同步。
- 不做管理员全局学习 BI。
- 不做服务端推送提醒。
- 不做新的调度算法。
- 不新建长期统计物化表，除非查询性能验证后确实需要。
- MVP 不做计划筛选、标签筛选、评分分布、完成率和导出报告。

## Technical Approach

推荐新增一个独立长期指标接口，例如 `GET /api/dashboard/long-term?date=YYYY-MM-DD&rangeDays=90`。

原因：

- 避免把现有 `/api/dashboard` 单日响应继续膨胀。
- 保留当前工作台的短路径加载速度。
- 前端可以把长期指标作为独立 query，失败时不影响今日主路径。

## Technical Notes

- Backend anchors:
  - `backend/src/main/java/com/jp/vocab/dashboard/service/StudyDashboardService.java`
  - `backend/src/main/java/com/jp/vocab/note/service/NoteDashboardService.java`
  - `backend/src/main/resources/db/migration/V3__init_card_instance.sql`
  - `backend/src/main/resources/db/migration/V7__init_note_review.sql`
  - `backend/src/main/resources/db/migration/V11__split_note_content_and_user_state.sql`
- Frontend anchors:
  - `frontend/src/features/dashboard/StudyDashboardPage.tsx`
  - `frontend/src/features/dashboard/api.ts`
  - `frontend/src/features/notes/api.ts`
- Specs:
  - `.trellis/spec/backend/current-feature-contracts.md`
  - `.trellis/spec/frontend/current-feature-workflows.md`
