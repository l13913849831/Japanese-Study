# implement: continuous review session experience

## Goal

把单词复习页和知识点复习页从当前的表格/管理台式交互，升级成更连续、更聚焦的复习会话体验。

## What I already know

* 单词复习页当前依赖手输或跨页带入 `Plan ID`，主界面是表格选卡后再提交评分。
* 知识点复习页当前也是队列表格 + 选中后右侧操作。
* 两条线都已有稳定的查询与评分接口，不必先改底层数据模型。

## Requirements

* 收敛出单词与知识点复习的统一交互语义。
* 降低表格选择和上下文切换成本。
* 强化“当前项 / 剩余项 / 进度 / 评分”主路径。

## Acceptance Criteria

* [x] 单词复习页主路径更像连续会话。
* [x] 知识点复习页主路径更像连续会话。
* [x] 两条线的复习进度表达更一致。

## Out of Scope

* 不强行合并单词线与知识点线的数据模型。

## Technical Notes

* 主要涉及：
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `frontend/src/features/review/session.ts`
  * `frontend/src/styles.css`

## Final Notes

* `/cards` 改成先选计划与日期，再进入当前卡片、进度、队列、历史四段式会话。
* `/notes/review` 改成标题回忆 -> 按需显示内容 -> 评分后继续前进的连续复习流。
* 两条线共享同一套会话索引与进度汇总辅助逻辑，但不强行合并底层数据模型。
