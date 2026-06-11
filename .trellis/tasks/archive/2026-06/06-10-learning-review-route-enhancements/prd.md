# roadmap: learning review route enhancements

## Goal

把学习复习路线后续增强沉淀成一个母任务。任务内区分两条线：复习体验线优先，基建线低优先级。复习线中的 P1 项拆成可独立执行的子任务，后续按子任务推进实现。

## What I already know

* 当前项目已经有单词线、知识点线、统一工作台、弱项池、长期指标 MVP、Web 端连续复习会话和小程序学习端。
* Web 端 `/cards` 和 `/notes/review` 已有回捞队列、恢复队列、弱项轮和 `sessionAgainCount`。
* 小程序 `/pages/cards/index` 和 `/pages/notes-review/index` 当前固定传 `sessionAgainCount: 0`，没有完整同日回收和弱项轮。
* 当前学习推荐逻辑集中在 `frontend/src/features/review/learningPath.ts`，主要按用户偏好和待复习数量排序。
* `docs/open-items.md` 已记录长周期指标完整增强、复习体验继续打磨和移动端布局等待排期项。
* 当前有两份前端：Web `frontend/` 和小程序 `miniapp/`。后续用户可见学习/复习变更必须同时评估两端。

## Completed Task Frontend Coverage Audit

* `06-10-p1-align-miniapp-review-sessions`: 覆盖小程序复习会话，并以 Web 规则为对齐目标；该任务主要补小程序缺口。
* `06-10-p1-learning-path-recommender-v2`: 主要覆盖 Web 工作台和 Web 复习完成后的推荐路径；小程序工作台未同步推荐器 v2。
* `06-10-p1-review-session-completion-recap`: 覆盖 Web 单词和知识点复习收尾页；该任务 PRD 明确把小程序收尾页排除在 MVP 外。

## Requirements

### 复习体验线

P1 子任务：

* `06-10-p1-align-miniapp-review-sessions`: 小程序复习会话对齐 Web 回收规则。
* `06-10-p1-learning-path-recommender-v2`: 今日学习推荐器 v2。
* `06-10-p1-review-session-completion-recap`: 复习会话完成收尾页。

P2 候选：

* `06-10-p2-weak-item-focused-review-mode`: 弱项专题复习模式，从 `/weak-items` 进入专门的补强会话，而不是只手动移出。
* `06-10-p2-new-word-load-auto-adjustment`: 新词负载自动调节，根据未来负载和积压给出 dailyNewCount 调整建议，必要时暂停新词引入。
* 长周期指标 MVP 已由 `06-10-long-term-learning-metrics` 完成；完整增强仍按 `docs/open-items.md` 独立排期。

P3 候选：

* `06-10-p3-link-words-notes-knowledge-points`: 单词和知识点联动，词条、例句、语法点和知识卡建立关联，支持从复习中沉淀知识点。

### 基建线

* 抽出可复用的复习会话队列逻辑，减少 Web 和小程序规则漂移。
* 为 Web 和小程序复习会话补回归测试，覆盖 AGAIN、回捞、弱项轮和完成态。
* 增加路由级冒烟测试，覆盖 `/dashboard`、`/cards`、`/notes/review`、`/weak-items`。
* 补 API 契约回归检查，重点覆盖 review response、todayAction、weak item summary。
* 本地工具固定使用：
  * Python: `C:\Users\luyh\AppData\Local\Python\bin\python.exe`
  * Maven: `D:\apache-maven-3.9.12\bin\mvn.cmd`

## Acceptance Criteria

* [x] 创建母任务并挂载复习线 P1 子任务。
* [x] 母任务 PRD 明确区分复习体验线和基建线。
* [x] 每个复习线 P1 子任务都有独立 PRD。
* [x] P1 子任务按独立任务推进时，不把 P2/P3 候选混入 MVP。
* [x] P1 子任务完成后，回到本母任务复盘 P2 排期。
* [x] 未完成的复习路线 P2/P3 候选已拆成独立子任务。
* [x] 已审查 P1 已完成任务是否覆盖 Web 和小程序。

## Definition of Done

* 子任务实现时补充对应测试或明确测试缺口。
* Web 前端执行 `npm run build`。
* 小程序相关改动执行 `npm run typecheck` 和 `npm run build:weapp`。
* 后端相关改动使用 `D:\apache-maven-3.9.12\bin\mvn.cmd test` 或更小范围测试。
* 行为变化同步更新 `.trellis/spec/` 和 `docs/system-usage-guide.md`。

## Out of Scope

* 本母任务不直接实现代码。
* 不优先做 Keycloak、S3 托管备份、管理员全局 BI。
* 不在 P1 阶段引入新的学习算法或机器学习推荐。

## Technical Notes

* Web review anchors:
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `frontend/src/features/review/session.ts`
  * `frontend/src/features/review/learningPath.ts`
* Miniapp review anchors:
  * `miniapp/src/pages/cards/index.tsx`
  * `miniapp/src/pages/notes-review/index.tsx`
  * `miniapp/src/pages/dashboard/index.tsx`
* Backend review anchors:
  * `backend/src/main/java/com/jp/vocab/card/service/CardReviewService.java`
  * `backend/src/main/java/com/jp/vocab/note/service/NoteReviewService.java`
  * `backend/src/main/java/com/jp/vocab/weakitem/service/WeakItemService.java`
* Existing docs:
  * `docs/open-items.md`
  * `docs/recommended-next-features.md`
  * `docs/system-usage-guide.md`
  * `.trellis/spec/frontend/current-feature-workflows.md`
  * `.trellis/spec/backend/current-feature-contracts.md`
