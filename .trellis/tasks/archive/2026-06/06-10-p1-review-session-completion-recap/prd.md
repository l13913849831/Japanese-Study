# P1: add review session completion recap

## Goal

把复习完成后的反馈从临时提示升级为明确的收尾页。用户完成单词或知识点会话后，应能看到本轮做了什么、表现如何、下一步做什么。

## Requirements

* Web `/cards` 完成主队列和弱项轮后显示会话收尾视图。
* Web `/notes/review` 完成主队列和弱项轮后显示会话收尾视图。
* 收尾视图展示：
  * 本轮总数。
  * 完成数。
  * AGAIN / HARD / GOOD / EASY 分布。
  * 平均响应耗时。
  * 回捞或恢复次数。
  * 新增弱项数量。
  * 本轮用时。
* 收尾视图提供下一步动作：
  * 回工作台。
  * 去另一条复习线。
  * 去弱项页。
  * 导出复盘材料。
* 本轮统计先存在前端会话态，不新增持久化表。
* 完成态不能阻塞已有 query invalidation。

## Acceptance Criteria

* [ ] 单词复习完成后不只显示空队列，而是显示本轮摘要。
* [ ] 知识点复习完成后不只显示空队列，而是显示本轮摘要。
* [ ] AGAIN / HARD / GOOD / EASY 统计和本轮点击一致。
* [ ] 弱项轮完成后摘要仍保留主队列和弱项轮总表现。
* [ ] 下一步动作与推荐器 v2 或现有推荐逻辑保持一致。
* [ ] Web 前端 build 通过。

## Definition of Done

* Web 单词线和知识点线使用一致的收尾展示模型。
* 统计逻辑不依赖重新拉取历史日志。
* 文档更新会话完成行为。

## Out of Scope

* 不持久化单次会话报表。
* 不生成长期分析图表；该能力属于长周期指标增强。
* 不优先做小程序收尾页；小程序先完成会话规则对齐。

## Technical Notes

* Web review pages:
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
* Shared helpers:
  * `frontend/src/features/review/session.ts`
  * `frontend/src/features/review/learningPath.ts`
* Export route:
  * `frontend/src/features/export-jobs/ExportJobPage.tsx`

## Decision

MVP 用前端会话态计算摘要。这样不改后端数据模型，也不会影响 FSRS 调度和 review log 合约。
