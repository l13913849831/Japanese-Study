# P1: align miniapp review sessions with web recovery rules

## Goal

让小程序单词复习和知识点复习对齐 Web 端的同日回收、弱项轮和后端 `sessionAgainCount` 合约。移动端是高频复习入口，不能让弱项沉淀和 Web 端规则不一致。

## Requirements

* 小程序单词复习支持 `MAIN`、`REQUEUE`、`WEAK` 三类会话行。
* 小程序知识点复习支持 `MAIN`、`RECOVERY`、`WEAK` 三类会话行。
* 小程序按 cardId / noteId 维护本次会话的 AGAIN 次数。
* 提交复习时传真实 `sessionAgainCount`，不再固定为 `0`。
* 根据后端 `todayAction` 更新本地会话队列：
  * word: `REQUEUE_TODAY` 进入回捞队列，`MOVE_TO_WEAK_ROUND` 进入弱项轮。
  * note: `MOVE_TO_RECOVERY_QUEUE` 进入恢复队列，`MOVE_TO_WEAK_ROUND` 进入弱项轮。
* 主队列完成且弱项轮非空时，提示用户进入弱项轮或暂时跳过。
* 提交成功后刷新今日队列、工作台、知识点看板和弱项摘要。
* 移动端布局保持轻量，不照搬 Web 的完整队列表格。

## Acceptance Criteria

* [ ] 小程序单词线第一次 AGAIN 会回到当日主队列尾部。
* [ ] 小程序单词线同一项第二次 AGAIN 会进入弱项轮，并传 `sessionAgainCount >= 2`。
* [ ] 小程序知识点线第一次 AGAIN 会进入恢复队列。
* [ ] 小程序知识点线同一项第二次 AGAIN 会进入弱项轮，并传 `sessionAgainCount >= 2`。
* [ ] 主队列结束后能进入弱项轮，弱项轮结束后显示完成态。
* [ ] 复习提交后弱项摘要能刷新。
* [ ] 小程序 `typecheck` 和 `build:weapp` 通过。

## Definition of Done

* 小程序复习路径和 Web 合约一致。
* 测试或手工回归覆盖 AGAIN、GOOD/EASY、弱项轮、空队列。
* 相关文档同步更新。

## Out of Scope

* 不新增后端接口。
* 不实现完整 Web 队列助手表格。
* 不做弱项专题复习模式；该能力放到 P2。

## Technical Notes

* Miniapp files:
  * `miniapp/src/pages/cards/index.tsx`
  * `miniapp/src/pages/notes-review/index.tsx`
  * `miniapp/src/features/cards/api.ts`
  * `miniapp/src/features/notes/api.ts`
* Web reference:
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `frontend/src/features/review/session.ts`
* Backend contract:
  * `CardReviewService#resolveTodayAction`
  * `NoteReviewService#resolveTodayAction`

## Decision

MVP 先复刻 Web 的会话态规则，但不把临时队列持久化到后端。后端继续只保存长期复习状态和弱项状态。
