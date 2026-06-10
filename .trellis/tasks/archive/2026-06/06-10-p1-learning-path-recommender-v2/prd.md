# P1: build learning path recommender v2

## Goal

把今日学习推荐从“按用户偏好 + 待复习数量”升级为更接近真实学习路线的推荐器。推荐结果要能解释为什么现在该做单词、知识点、弱项，或可以收尾。

## Requirements

* 保留用户默认学习顺序作为 tie-breaker。
* 推荐器输入至少包含：
  * 单词待复习数。
  * 知识点待复习数。
  * 弱项总数。
  * 未来 7 天负载。
  * 当日已完成量。
* 推荐器输出包含：
  * 推荐路线：`WORD` / `NOTE` / `WEAK` / `DONE`。
  * 后续路线。
  * 推荐理由。
  * 风险等级：正常 / 积压 / 过载。
* `/dashboard` 展示推荐路线和原因。
* `/cards` 和 `/notes/review` 完成后复用推荐器判断下一步。
* 第一版优先在前端组合现有接口，不新增后端聚合接口。

## Acceptance Criteria

* [ ] 单词和知识点都待复习时，推荐器能按用户偏好排序。
* [ ] 存在弱项且主路线已完成时，推荐弱项。
* [ ] 未来 7 天负载过高时，推荐理由能提示负载风险。
* [ ] `/dashboard` 的推荐动作和会话完成后的下一步动作一致。
* [ ] 不影响当前 `preferredLearningOrder` 设置。
* [ ] Web 前端 build 通过。

## Definition of Done

* 推荐逻辑集中在可测试、可复用的模块中。
* Dashboard 和复习会话不各自复制判断逻辑。
* 文档说明推荐器第一版仍是前端编排，不是后端学习计划引擎。

## Out of Scope

* 不做机器学习推荐。
* 不持久化每日推荐结果。
* 不新增通知、提醒或推送。
* 不自动修改学习计划参数；新词负载调节属于 P2。

## Technical Notes

* Current recommender:
  * `frontend/src/features/review/learningPath.ts`
* Call sites:
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
* Data sources:
  * `getStudyDashboard`
  * `getNoteDashboard`
  * `getWeakItemSummary`
  * `getLongTermDashboard`

## Decision

MVP 采用前端推荐器。原因是现有工作台已经组合多个查询，先集中前端规则成本最低。后续如果推荐逻辑需要跨端复用或稳定审计，再提升为后端聚合接口。
