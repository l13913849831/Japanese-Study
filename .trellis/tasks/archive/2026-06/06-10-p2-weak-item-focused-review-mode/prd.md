# P2: weak item focused review mode

## Goal

把 `/weak-items` 从“查看和手动移出薄弱项”增强为可进入专题补强会话的入口。用户可以只练当前薄弱单词和薄弱知识点，形成独立于今日主队列的补强路径。

## What I already know

* 当前系统已有单词线、知识点线、弱项池和连续复习会话。
* `/weak-items` 已能展示薄弱单词、薄弱知识点，并支持手动移出。
* P1 已完成 Web 和小程序复习会话的弱项轮规则对齐。
* 母任务把“弱项专题复习模式”列为 P2 候选，不应混入 P1 MVP。

## Requirements

* 在 Web `/weak-items` 和小程序 `/pages/weak-items/index` 提供进入专题补强会话的入口。
* 专题会话覆盖薄弱单词和薄弱知识点。
* 会话优先复用现有单词 / 知识点复习提交合约，不新增独立评分体系。
* 用户完成补强后，能看到本轮完成态和下一步动作。
* 补强成功后，弱项摘要和弱项列表应刷新。
* 空弱项状态仍保留，不显示无意义的开始按钮。

## Acceptance Criteria

* [ ] 有薄弱单词时，用户能从 Web `/weak-items` 进入单词薄弱项补强会话。
* [ ] 有薄弱知识点时，用户能从 Web `/weak-items` 进入知识点薄弱项补强会话。
* [ ] 有薄弱单词时，用户能从小程序弱项页进入单词薄弱项补强会话。
* [ ] 有薄弱知识点时，用户能从小程序弱项页进入知识点薄弱项补强会话。
* [ ] `GOOD` / `EASY` 后能沿用现有弱项毕业规则。
* [ ] 补强会话结束后，弱项摘要和列表状态刷新。
* [ ] 无薄弱项时，页面仍显示可理解的空状态。
* [ ] Web 前端 build 通过。
* [ ] 小程序 typecheck 和 build:weapp 通过。

## Definition of Done

* 测试或手工回归覆盖有弱项、无弱项、补强完成、弱项毕业。
* 行为变化同步更新 `.trellis/spec/` 和 `docs/system-usage-guide.md`。
* 不破坏 `/cards`、`/notes/review` 的主复习会话。

## Out of Scope

* 不做新的弱项判定算法。
* 不做长期弱项统计报表。
* 不做新建后端持久化会话表。
* 不做新的小程序页面层级；优先复用现有弱项页和复习页。

## Technical Notes

* Weak item page:
  * `frontend/src/features/weak-items/WeakItemsPage.tsx`
  * `frontend/src/features/weak-items/api.ts`
  * `miniapp/src/pages/weak-items/index.tsx`
  * `miniapp/src/features/weak-items/api.ts`
* Review session references:
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `frontend/src/features/review/session.ts`
* Backend weak item contract:
  * `backend/src/main/java/com/jp/vocab/weakitem/controller/WeakItemController.java`
  * `backend/src/main/java/com/jp/vocab/weakitem/service/WeakItemService.java`
* Specs:
  * `.trellis/spec/backend/current-feature-contracts.md`
  * `.trellis/spec/frontend/current-feature-workflows.md`

## Decision

MVP 同时覆盖 Web 和小程序弱项入口。原因是两端都已经有弱项页，专题补强属于用户可见复习路径，不能只做其中一端。
